package com.electriccloud.listeners


import io.qameta.allure.Attachment
import org.apache.log4j.Logger
import org.testng.ITestResult
import org.testng.reporters.ExitCodeListener

import static com.google.common.io.Files.toByteArray
import static com.electriccloud.helpers.config.ConfigHelper.message

class TestListener extends ExitCodeListener {

    public static Logger log = Logger.getLogger('appLogger')


    static def logDir = "src/main/resources/logs"
    static def logFileName = "allureLog"

    @Override
    void onTestFailure(ITestResult result) {
        super.onTestFailure(result)
        message('Test is completed with failure', '=', '*')
        attachLog()
    }

    @Override
    void onTestSkipped(ITestResult result) {
        super.onTestSkipped(result)
        message('Test is skipped', '=', '*')
        attachLog()

    }

    @Override
    void onTestSuccess(ITestResult result) {
        super.onTestSuccess(result)
        message('Test is completed with success', '=', '*')
        attachLog()
    }


    @Override
    void onTestStart(ITestResult result) {
        super.onTestStart(result)
        message(' Test is started', '=', '*')
        cleanUpLog(logFileName)
    }


    static void cleanUpLog(fileName) {
        new File("${logDir}/${fileName}.log").withWriter { writer -> writer.write('') }
    }

    @Attachment(value = 'TestLog')
    static def attachLog(){
        log.info("Taking log to allure report")
        def file = new File("${logDir}/${logFileName}.log")
        toByteArray(file)
    }




}
