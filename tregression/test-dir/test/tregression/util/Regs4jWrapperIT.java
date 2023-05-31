package tregression.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import core.Migrator;
import core.Reducer;
import core.SourceCodeManager;
import model.Regression;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

class Regs4jWrapperIT {
    private Regs4jWrapper wrapper;

    @BeforeEach
    void setUp() {
        SourceCodeManager sourceCodeManager = new SourceCodeManager();
        Reducer reducer = new Reducer();
        Migrator migrator = new Migrator();
        wrapper = new Regs4jWrapper(sourceCodeManager, reducer, migrator);
    }

    @Test
    void checkout_TestCaseIsEmpty_FailsCheckoutWithoutCrashing() {
        Regression regression = new Regression();
        regression.setTestCase("");
        assertFalse(wrapper.checkout("projectName", regression, null));
    }
    
    @Test
    void checkout_CheckoutFailureDueToConflictingFiles_ReturnsFalse() {
        // cflint/CFLint project with regression 1 results in checkout failure due to "conflicting files" as of 31st May 2023
        // We could mock the SourceCodeManager to return null for the SourceCodeManager#checkout to simulate this behaviour.
        String projectName = "cflint/CFLint";
        List<Regression> regressions = wrapper.getRegressions(projectName);
        assertFalse(wrapper.checkout(projectName, regressions.get(0), wrapper.generateProjectPaths("repoPath", projectName, 1)));
    }
}
