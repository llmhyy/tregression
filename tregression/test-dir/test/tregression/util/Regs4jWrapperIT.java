package tregression.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import core.Migrator;
import core.Reducer;
import core.SourceCodeManager;
import model.Regression;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
}
