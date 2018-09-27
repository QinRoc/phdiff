package io.nthienan.phdiff

import io.nthienan.phdiff.report.RemarkupGlobalReportBuilder
import io.nthienan.phdiff.report.RemarkupInlineReportBuilder
import io.nthienan.phdiff.report.RemarkupUtils
import org.sonar.api.Plugin
import org.sonar.api.Properties
import org.sonar.api.Property
import org.sonar.api.PropertyType
import org.sonar.api.utils.log.Loggers



/**
 * Created on 29-Jul-17.
 * @author nthienan
 */
@Properties(
  Property(
    key = PhabricatorDifferentialPlugin.PHABRICATOR_URL,
    name = "Phabricator URL",
    description = "URL to access Phabricator.",
    defaultValue = "http://localhost",
    project = true
  ),
  Property(
    key = PhabricatorDifferentialPlugin.CONDUIT_TOKEN,
    name = "Conduit token",
    description = "Conduit token",
    project = true,
    type = PropertyType.PASSWORD
  ),
  Property(
    key = PhabricatorDifferentialPlugin.DIFF_ID,
    name = "DIFF_ID",
    description = "Diff ID",
    global = false
  )
)

class PhabricatorDifferentialPlugin : Plugin {

  private val log = Loggers.get(PhabricatorDifferentialPlugin::class.java)

  companion object {
    const val PHABRICATOR_URL = "sonar.phdiff.phabricatorUrl"
    const val DIFF_ID = "sonar.phdiff.diffId"
    const val CONDUIT_TOKEN = "sonar.phdiff.conduitToken"
  }

  override fun define(context: Plugin.Context?) {
    log.debug("---add context {} start---",context.toString())

    context?.addExtensions(listOf(
      PhabricatorDifferentialPostJob::class.java,
      Configuration::class.java,
      RemarkupGlobalReportBuilder::class.java,
      RemarkupUtils::class.java,
      RemarkupInlineReportBuilder::class.java))

    log.debug("---add context {} success!---",context.toString())
  }

}
