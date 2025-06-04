package nl.inholland.bankAppBackEnd;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "nl.inholland.bankAppBackEnd,nl.inholland.bankAppBackEnd.stepdefinitions")
public class CucumberTestRunner {
}
