package io.nthienan.phdiff

import io.nthienan.phdiff.conduit.ConduitClient
import io.nthienan.phdiff.conduit.ConduitException
import io.nthienan.phdiff.conduit.DifferentialClient
import io.nthienan.phdiff.issue.IssueComparator
import io.nthienan.phdiff.report.*
import org.sonar.api.batch.postjob.PostJob
import org.sonar.api.batch.postjob.PostJobContext
import org.sonar.api.batch.postjob.PostJobDescriptor
import org.sonar.api.config.Settings
import org.sonar.api.utils.System2
import org.sonar.api.utils.log.Loggers
import java.util.stream.StreamSupport

/**
 * Compute comments to be added on the differential.
 *
 * PostJob: when the scanner sends the raw report to Compute Engine
 *
 * @author nthienan, qinpeng
 */
class PhabricatorDifferentialPostJob(
  settings:Settings,
  system2: System2
) : PostJob {

  private val differentialClient: DifferentialClient
  private val projectKey: String
  var globalReportBuilder: GlobalReportBuilder? = null
  var inlineReportBuilder: InlineReportBuilder? = null
  var configuration: Configuration? = null

  init {
    log.debug("initial RemarkupUtils")
    val remarkupUtils = RemarkupUtils(settings)

    log.debug("initial globalReportBuilder")
    this.globalReportBuilder = RemarkupGlobalReportBuilder(remarkupUtils)

    log.debug("initial inlineReportBuilder")
    this.inlineReportBuilder = RemarkupInlineReportBuilder(remarkupUtils)

    log.debug("initial configuration")
    this.configuration = Configuration(settings, system2)

    log.info("initial PhabricatorDifferentialPostJob start: Configuration:{}",configuration.toString())

    val url = configuration!!.phabricatorUrl()
    log.debug("url={}",url)
    val token = configuration!!.conduitToken()
    log.debug("token={}",token)
    this.differentialClient = DifferentialClient(ConduitClient(url, token))
    log.debug("differentialClient={}",differentialClient)

    this.projectKey = configuration!!.projectKey()
    log.debug("projectKey={}",projectKey)

    log.info("initial PhabricatorDifferentialPostJob end: Configuration:{}",configuration.toString())
  }

  companion object {
    private val issueComparator = IssueComparator()
    private val log = Loggers.get(PhabricatorDifferentialPostJob::class.java)
  }

  override fun describe(descriptor: PostJobDescriptor?) {
    descriptor
      ?.name("Publish issues")
      ?.requireProperty(PhabricatorDifferentialPlugin.DIFF_ID)
  }

  override fun execute(context: PostJobContext?) {
    log.info("Analysis result prepare to publish to your differential revision")
    val diffID = configuration!!.diffId()
    try {
      val diff = differentialClient.fetchDiff(diffID)
      if (context?.analysisMode()?.isIssues == true) {
        StreamSupport.stream(context.issues()?.spliterator(), false)
          .filter { it.isNew }
          .filter { it.inputComponent()?.isFile ?: false }
          .sorted(issueComparator)
          .forEach { i ->
            run {
              globalReportBuilder!!.add(i)
              val ic = inlineReportBuilder!!.issue(i).build()
              val filePath = i.componentKey().replace(projectKey, "").substring(1)
              try {
                differentialClient.postInlineComment(diffID, filePath, i.line()!!, ic)
                log.debug("Comment $ic has been published")
              } catch (e: ConduitException) {
                if (e.message.equals("Requested file doesn't exist in this revision.")) {
                  val message = "Unmodified file $filePath  on line ${i.line()}\n\n $ic"
                  differentialClient.postComment(diff.revisionId, message, false)
                } else {
                  log.error("Publish comment error", e)
                }
              }
          }
        }
      }
      differentialClient.postComment(diff.revisionId, globalReportBuilder!!.summarize())
      log.info("Analysis result has been published to your differential revision")
    } catch (e: ConduitException) {
      log.error("Publish analysis result to differential revision fail!", e)
    }
  }
}
