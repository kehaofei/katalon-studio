package com.kms.katalon.execution.generator;

import groovy.text.GStringTemplateEngine
import groovy.transform.CompileStatic

import com.kms.katalon.core.configuration.RunConfiguration;
import com.kms.katalon.core.driver.DriverCleanerCollector
import com.kms.katalon.core.exception.StepFailedException
import com.kms.katalon.core.keyword.IKeywordContributor
import com.kms.katalon.core.logging.KeywordLogger
import com.kms.katalon.core.main.TestCaseMain
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.reporting.ReportUtil
import com.kms.katalon.core.testcase.TestCaseBinding
import com.kms.katalon.core.testdata.TestDataColumn
import com.kms.katalon.custom.factory.BuiltInMethodNodeFactory
import com.kms.katalon.entity.testsuite.TestSuiteEntity
import com.kms.katalon.execution.configuration.IRunConfiguration;
import com.kms.katalon.execution.entity.TestCaseExecutedEntity
import com.kms.katalon.execution.entity.TestSuiteExecutedEntity
import com.kms.katalon.execution.util.ExecutionUtil
import com.kms.katalon.groovy.util.GroovyStringUtil

@CompileStatic
public class TestSuiteScriptTemplate {
    private static final String tpl ='''<% importNames.each { %>import <%= it %>
<% } %>

<% testCaseIds.eachWithIndex { item, index -> %>
def static runTestCase_<%= index %>() {
    TestCaseMain.runTestCase('<%= item %>', <%= testCaseBindings.get(index) %>, FailureHandling.STOP_ON_FAILURE)
}
<% } %>

Map<String, String> suiteProperties = new HashMap<String, String>();

<% configProperties.each { k, v -> %>
suiteProperties.put('<%= k %>', '<%= v %>')
<% } %> 

<% driverCleaners.each { %>DriverCleanerCollector.getInstance().addDriverCleaner(new <%= it %>())
<% } %>


RunConfiguration.setExecutionSettingFile("<%= executionConfigFilePath %>")

TestCaseMain.beforeStart()

KeywordLogger.getInstance().startSuite('<%= testSuite.getName() %>', suiteProperties)

(0..<%= testCaseIds.size() - 1 %>).each {
    "<%= trigger %>"()
}

DriverCleanerCollector.getInstance().cleanDriversAfterRunningTestSuite()
KeywordLogger.getInstance().endSuite('<%= testSuite.getName() %>', null)
'''
    @CompileStatic
    def static generateTestSuiteScriptFile(File file, TestSuiteEntity testSuite, List<String> testCaseBindings,
            IRunConfiguration runConfig, TestSuiteExecutedEntity testSuiteExecutedEntity) {

        def importNames = [
            KeywordLogger.class.getName(),
            StepFailedException.class.getName(),
            ReportUtil.class.getName(),
            TestCaseMain.class.getName(),
            TestDataColumn.class.getName(),
            MissingPropertyException.class.getName(),
            TestCaseBinding.class.getName(),
            DriverCleanerCollector.class.getName(),
            FailureHandling.class.getName(),
            RunConfiguration.class.getName()
        ]


        def driverCleaners = []
        for (IKeywordContributor contributor in BuiltInMethodNodeFactory.getInstance().getKeywordContributors()) {
            if (contributor.getDriverCleaner() != null) {
                driverCleaners.add(contributor.getDriverCleaner().getName())
            }
        }

        List<String> testCaseIds = new ArrayList<String>();
        for (TestCaseExecutedEntity testCaseExecutedEntity in testSuiteExecutedEntity.getTestCaseExecutedEntities()) {
            for (int index = 0; index < testCaseExecutedEntity.getLoopTimes(); index++) {
                testCaseIds.add(testCaseExecutedEntity.getTestCaseId())
            }
        }

        def binding = [
            "importNames": importNames,
            "testSuite" : testSuite,
            "testCaseIds": testCaseIds,
            "testCaseBindings": testCaseBindings,
            "configProperties" : ExecutionUtil.escapeGroovy(testSuiteExecutedEntity.getAttributes()),
            "executionConfigFilePath" : GroovyStringUtil.escapeGroovy(runConfig.getExecutionSetting().getSettingFilePath()),
            "driverCleaners" : driverCleaners,
            "trigger": 'runTestCase_${it}'
        ]

        def engine = new GStringTemplateEngine()
        def tpl = engine.createTemplate(tpl).make(binding)
        if (file != null) {
            if (file.canWrite()) {
                file.write(tpl.toString())
            }
        } else {
            return tpl.toString()
        }
    }
}
