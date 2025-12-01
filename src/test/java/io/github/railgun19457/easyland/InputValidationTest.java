package io.github.railgun19457.easyland;

import io.github.railgun19457.easyland.util.InputValidator;
import io.github.railgun19457.easyland.util.ValidationResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Input validation test class.
 * Tests the various validation functions of the InputValidator utility class.
 */
public class InputValidationTest {

    @Test
    public void testValidPlayerName() {
        // Test valid player names
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validatePlayerName("Player123"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validatePlayerName("test_player"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validatePlayerName("abc"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validatePlayerName("1234567890123456"));
    }

    @Test
    public void testInvalidPlayerName() {
        // Test invalid player names
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validatePlayerName(""));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validatePlayerName(null));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validatePlayerName("ab")); // Too short
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validatePlayerName("Player-123"));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validatePlayerName("Player@123"));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validatePlayerName("Player 123"));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validatePlayerName("12345678901234567")); // Too long
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validatePlayerName("test\nname")); // Contains control characters
    }

    @Test
    public void testValidLandId() {
        // Test valid land IDs
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateLandId("land123"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateLandId("test_land"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateLandId("A"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateLandId("12345678901234567890123456789012"));
    }

    @Test
    public void testInvalidLandId() {
        // Test invalid land IDs
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateLandId(""));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateLandId(null));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateLandId("land-123"));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateLandId("land@123"));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateLandId("land 123"));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateLandId("123456789012345678901234567890123")); // Too long
    }

    @Test
    public void testValidUUID() {
        // Test valid UUIDs
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateUUID("123e4567-e89b-12d3-a456-426614174000"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateUUID("00000000-0000-0000-0000-000000000000"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateUUID("ffffffff-ffff-ffff-ffff-ffffffffffff"));
    }

    @Test
    public void testInvalidUUID() {
        // Test invalid UUIDs
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateUUID(""));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateUUID(null));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateUUID("123e4567-e89b-12d3-a456-42661417400")); // Missing a digit
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateUUID("123e4567e89b-12d3-a456-426614174000")); // Missing hyphen
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateUUID("ggge4567-e89b-12d3-a456-426614174000")); // Invalid character
    }

    @Test
    public void testValidBoolean() {
        // Test valid boolean strings
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateBoolean("true"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateBoolean("false"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateBoolean("on"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateBoolean("off"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateBoolean("enable"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateBoolean("disable"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateBoolean("TRUE"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateBoolean("FALSE"));
    }

    @Test
    public void testInvalidBoolean() {
        // Test invalid boolean strings
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateBoolean(""));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateBoolean(null));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateBoolean("yes"));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateBoolean("no"));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateBoolean("1"));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateBoolean("0"));
    }

    @Test
    public void testParseBoolean() {
        // Test boolean parsing
        assertTrue(InputValidator.parseBoolean("true"));
        assertTrue(InputValidator.parseBoolean("on"));
        assertTrue(InputValidator.parseBoolean("enable"));
        assertTrue(InputValidator.parseBoolean("TRUE"));

        assertFalse(InputValidator.parseBoolean("false"));
        assertFalse(InputValidator.parseBoolean("off"));
        assertFalse(InputValidator.parseBoolean("disable"));
        assertFalse(InputValidator.parseBoolean("FALSE"));

        assertFalse(InputValidator.parseBoolean(null));
        assertFalse(InputValidator.parseBoolean(""));
        assertFalse(InputValidator.parseBoolean("invalid"));
    }

    @Test
    public void testSqlSafe() {
        // Test SQL safe validation
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateSqlSafe("some_parameter"));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateSqlSafe(""));
        assertInstanceOf(ValidationResult.Success.class, InputValidator.validateSqlSafe(null));

        // Test with control characters
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateSqlSafe("test\u0001"));
        assertInstanceOf(ValidationResult.Failure.class, InputValidator.validateSqlSafe("test\b"));
    }

    @Test
    public void testValidationResultRecords() {
        // Test ValidationResult records
        ValidationResult success = new ValidationResult.Success();
        assertInstanceOf(ValidationResult.Success.class, success);

        ValidationResult failure = new ValidationResult.Failure("Error message");
        assertInstanceOf(ValidationResult.Failure.class, failure);
        assertEquals("Error message", ((ValidationResult.Failure) failure).errorMessage());
    }
}