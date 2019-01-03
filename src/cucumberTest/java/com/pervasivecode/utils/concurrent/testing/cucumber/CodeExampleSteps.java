package com.pervasivecode.utils.concurrent.testing.cucumber;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;
import java.io.PrintWriter;
import java.io.StringWriter;
import com.pervasivecode.utils.concurrent.chute.example.ExampleApplication;
import com.pervasivecode.utils.concurrent.chute.example.ParallelVirusScannerExample;
import com.pervasivecode.utils.concurrent.chute.example.SingleThreadedVirusScannerExample;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class CodeExampleSteps {
  private String commandOutput = "";
  private ExampleApplication codeExample = null;

  @Given("^I am running the Parallel Virus Scanner Example$")
  public void iAmRunningTheParallelVirusScannerExample() {
    iAmRunningTheExample(new ParallelVirusScannerExample(false));
  }

  @Given("^I am running the Parallel Virus Scanner Example with Verbose Output$")
  public void iAmRunningTheParallelVirusScannerExampleWithVerboseOutput() {
    iAmRunningTheExample(new ParallelVirusScannerExample(true));
  }

  @Given("^I am running the Single-Threaded Virus Scanner Example$")
  public void iAmRunningTheSingleThreadedVirusScannerExample() {
    iAmRunningTheExample(new SingleThreadedVirusScannerExample(false));
  }

  @Given("^I am running the Single-Threaded Virus Scanner Example with Verbose Output$")
  public void iAmRunningTheSingleThreadedVirusScannerExampleWithVerboseOutput() {
    iAmRunningTheExample(new SingleThreadedVirusScannerExample(true));
  }

  private void iAmRunningTheExample(ExampleApplication exampleClass) {
    this.codeExample = exampleClass;
    this.commandOutput = "";
  }

  @When("^I run the program$")
  public void iRunTheProgram() throws Exception {
    checkNotNull(this.codeExample, "did you forget an 'I am running the' example step?");
    StringWriter sw = new StringWriter();
    this.codeExample.runExample(new PrintWriter(sw, true));
    commandOutput = commandOutput.concat(sw.toString());
  }

  @Then("^I should see the output$")
  public void iShouldSeeTheOutput(String expected) {
    assertThat(this.commandOutput).isEqualTo(expected);
  }
}
