package com.example.Calculator.Service;

import org.springframework.stereotype.Service;

@Service
public class CalculatorService {

    /*
     * Default mode = DEG
     * Compatibility ke liye ye method bhi rakha hai.
     */
    public double calculate(String expression) {
        return calculate(expression, "DEG");
    }

    /*
     * Main calculation method
     */
    public double calculate(String expression, String angleMode) {

        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }

        if (angleMode == null ||
                (!angleMode.equalsIgnoreCase("DEG")
                        && !angleMode.equalsIgnoreCase("RAD"))) {

            angleMode = "DEG";
        }

        Parser parser = new Parser(expression, angleMode);

        double result = parser.parseExpression();

        parser.skipSpaces();

        if (parser.hasRemaining()) {
            throw new IllegalArgumentException(
                    "Unexpected character at position "
                            + parser.getPosition()
            );
        }

        return result;
    }


    // =========================================================
    // PARSER
    // =========================================================

    private static class Parser {

        private final String expression;
        private final String angleMode;

        private int position = 0;

        Parser(String expression, String angleMode) {
            this.expression = expression;
            this.angleMode = angleMode;
        }


        // =====================================================
        // expression
        // + -
        // =====================================================

        double parseExpression() {

            double result = parseTerm();

            while (true) {

                skipSpaces();

                if (match('+')) {

                    double right = parseTerm();
                    result += right;

                } else if (match('-')) {

                    double right = parseTerm();
                    result -= right;

                } else {
                    break;
                }
            }

            return result;
        }


        // =====================================================
        // term
        // * /
        // =====================================================

        private double parseTerm() {

            double result = parsePower();

            while (true) {

                skipSpaces();

                if (match('*')) {

                    double right = parsePower();
                    result *= right;

                } else if (match('/')) {

                    double right = parsePower();

                    if (right == 0) {
                        throw new ArithmeticException(
                                "Cannot Divide By Zero"
                        );
                    }

                    result /= right;

                } else {
                    break;
                }
            }

            return result;
        }


        // =====================================================
        // power
        // ^
        // =====================================================

        private double parsePower() {

            double result = parseUnary();

            skipSpaces();

            /*
             * Right associative:
             *
             * 2^3^2
             *
             * = 2^(3^2)
             * = 512
             */
            if (match('^')) {

                double exponent = parsePower();

                result = Math.pow(result, exponent);
            }

            return result;
        }


        // =====================================================
        // unary
        // + -
        // √
        // =====================================================

        private double parseUnary() {

            skipSpaces();

            if (position >= expression.length()) {
                throw new IllegalArgumentException(
                        "Incomplete expression"
                );
            }

            char current = expression.charAt(position);


            // Positive
            if (current == '+') {

                position++;

                return parseUnary();
            }


            // Negative
            if (current == '-') {

                position++;

                return -parseUnary();
            }


            // Square Root
            if (current == '√') {

                position++;

                double number = parseUnary();

                if (number < 0) {

                    throw new ArithmeticException(
                            "Cannot calculate square root of negative number"
                    );
                }

                return Math.sqrt(number);
            }


            return parsePostfix();
        }


        // =====================================================
        // postfix
        // !
        // =====================================================

        private double parsePostfix() {

            double result = parsePrimary();

            while (true) {

                skipSpaces();

                if (match('!')) {

                    result = factorial(result);

                } else {
                    break;
                }
            }

            return result;
        }


        // =====================================================
        // primary
        // number
        // brackets
        // functions
        // constants
        // =====================================================

        private double parsePrimary() {

            skipSpaces();

            if (position >= expression.length()) {

                throw new IllegalArgumentException(
                        "Expected number at position "
                                + position
                );
            }


            char current = expression.charAt(position);


            // =================================================
            // Brackets
            // =================================================

            if (current == '(') {

                position++;

                double result = parseExpression();

                skipSpaces();

                if (!match(')')) {

                    throw new IllegalArgumentException(
                            "Missing closing bracket"
                    );
                }

                return result;
            }


            // =================================================
            // Constants
            // =================================================

            if (current == 'π') {

                position++;

                return Math.PI;
            }


            if (current == 'e') {

                position++;

                return Math.E;
            }


            // =================================================
            // Functions
            // =================================================

            if (isLetter(current)) {

                return parseFunction();
            }


            // =================================================
            // Number
            // =================================================

            if (Character.isDigit(current) || current == '.') {

                return parseNumber();
            }


            throw new IllegalArgumentException(
                    "Expected number at position "
                            + position
            );
        }


        // =====================================================
        // FUNCTION
        // =====================================================

        private double parseFunction() {

            String functionName = parseFunctionName();

            skipSpaces();


            if (!match('(')) {

                throw new IllegalArgumentException(
                        functionName + " requires parentheses"
                );
            }


            double value = parseExpression();

            skipSpaces();


            if (!match(')')) {

                throw new IllegalArgumentException(
                        "Missing closing bracket for "
                                + functionName
                );
            }


            return applyFunction(functionName, value);
        }


        // =====================================================
        // FUNCTION NAME
        // =====================================================

        private String parseFunctionName() {

            int start = position;

            while (position < expression.length()
                    && isLetter(expression.charAt(position))) {

                position++;
            }

            return expression
                    .substring(start, position)
                    .toLowerCase();
        }


        // =====================================================
        // APPLY FUNCTION
        // =====================================================

        private double applyFunction(
                String function,
                double value) {

            switch (function) {

                // =============================================
                // SIN
                // =============================================

                case "sin":

                    if (angleMode.equalsIgnoreCase("DEG")) {

                        return Math.sin(
                                Math.toRadians(value)
                        );

                    } else {

                        return Math.sin(value);
                    }


                // =============================================
                // COS
                // =============================================

                case "cos":

                    if (angleMode.equalsIgnoreCase("DEG")) {

                        return Math.cos(
                                Math.toRadians(value)
                        );

                    } else {

                        return Math.cos(value);
                    }


                // =============================================
                // TAN
                // =============================================

                case "tan":

                    if (angleMode.equalsIgnoreCase("DEG")) {

                        double radians =
                                Math.toRadians(value);

                        /*
                         * 90°, 270°, etc. par tan undefined hota hai.
                         */
                        if (Math.abs(Math.cos(radians)) < 1e-12) {

                            throw new ArithmeticException(
                                    "Tan is undefined for this angle"
                            );
                        }

                        return Math.tan(radians);

                    } else {

                        if (Math.abs(Math.cos(value)) < 1e-12) {

                            throw new ArithmeticException(
                                    "Tan is undefined for this angle"
                            );
                        }

                        return Math.tan(value);
                    }


                // =============================================
                // LOG base 10
                // =============================================

                case "log":

                    if (value <= 0) {

                        throw new ArithmeticException(
                                "Log is defined only for positive numbers"
                        );
                    }

                    return Math.log10(value);


                // =============================================
                // LN
                // =============================================

                case "ln":

                    if (value <= 0) {

                        throw new ArithmeticException(
                                "Ln is defined only for positive numbers"
                        );
                    }

                    return Math.log(value);


                // =============================================
                // ASIN
                // =============================================

                case "asin":

                    if (value < -1 || value > 1) {

                        throw new ArithmeticException(
                                "Asin input must be between -1 and 1"
                        );
                    }

                    if (angleMode.equalsIgnoreCase("DEG")) {

                        return Math.toDegrees(
                                Math.asin(value)
                        );

                    } else {

                        return Math.asin(value);
                    }


                // =============================================
                // ACOS
                // =============================================

                case "acos":

                    if (value < -1 || value > 1) {

                        throw new ArithmeticException(
                                "Acos input must be between -1 and 1"
                        );
                    }

                    if (angleMode.equalsIgnoreCase("DEG")) {

                        return Math.toDegrees(
                                Math.acos(value)
                        );

                    } else {

                        return Math.acos(value);
                    }


                // =============================================
                // ATAN
                // =============================================

                case "atan":

                    if (angleMode.equalsIgnoreCase("DEG")) {

                        return Math.toDegrees(
                                Math.atan(value)
                        );

                    } else {

                        return Math.atan(value);
                    }


                default:

                    throw new IllegalArgumentException(
                            "Unknown function: "
                                    + function
                    );
            }
        }


        // =====================================================
        // NUMBER
        // =====================================================

        private double parseNumber() {

            skipSpaces();

            int start = position;

            boolean decimalFound = false;

            while (position < expression.length()) {

                char current =
                        expression.charAt(position);


                if (Character.isDigit(current)) {

                    position++;

                } else if (current == '.') {

                    if (decimalFound) {

                        throw new IllegalArgumentException(
                                "Invalid number"
                        );
                    }

                    decimalFound = true;

                    position++;

                } else {

                    break;
                }
            }


            if (start == position) {

                throw new IllegalArgumentException(
                        "Expected number at position "
                                + position
                );
            }


            String number =
                    expression.substring(start, position);


            try {

                return Double.parseDouble(number);

            } catch (NumberFormatException e) {

                throw new IllegalArgumentException(
                        "Invalid number: " + number
                );
            }
        }


        // =====================================================
        // FACTORIAL
        // =====================================================

        private double factorial(double number) {

            if (number < 0) {

                throw new ArithmeticException(
                        "Factorial is not defined for negative numbers"
                );
            }


            if (number != Math.floor(number)) {

                throw new ArithmeticException(
                        "Factorial requires a whole number"
                );
            }


            if (number > 170) {

                throw new ArithmeticException(
                        "Number too large for factorial"
                );
            }


            double result = 1;


            for (int i = 2; i <= (int) number; i++) {

                result *= i;
            }


            return result;
        }


        // =====================================================
        // HELPERS
        // =====================================================

        private boolean match(char expected) {

            if (position < expression.length()
                    && expression.charAt(position) == expected) {

                position++;

                return true;
            }

            return false;
        }


        private void skipSpaces() {

            while (position < expression.length()
                    && Character.isWhitespace(
                            expression.charAt(position))) {

                position++;
            }
        }


        private boolean hasRemaining() {

            skipSpaces();

            return position < expression.length();
        }


        private int getPosition() {

            return position;
        }


        private boolean isLetter(char c) {

            return Character.isLetter(c);
        }
    }
}