package com.phantomstr.testing.tool.swagger2retrofit;

class CamelCaseUtils {

    static String toCamelCase(String string, boolean firstWordToLowerCase) {
        char currentChar, previousChar = '\u0000';
        StringBuilder result = new StringBuilder();

        boolean firstLetterArrived = !firstWordToLowerCase;
        boolean nextLetterInUpperCase = true;

        for (int i = 0; i < string.length(); i++) {
            currentChar = string.charAt(i);

            if (!Character.isLetterOrDigit(currentChar) || (
                    ((Character.isLetter(previousChar) && Character.isLowerCase(previousChar)) || Character.isDigit(previousChar)) &&
                            Character.isLetter(currentChar) && Character.isUpperCase(currentChar))
            ) {
                nextLetterInUpperCase = true;
                if (!Character.isLetterOrDigit(currentChar)) {
                    previousChar = currentChar;
                    continue;
                }
            }

            if (nextLetterInUpperCase && firstLetterArrived) {
                result.append(Character.toUpperCase(currentChar));
            } else {
                result.append(Character.toLowerCase(currentChar));
            }

            firstLetterArrived = true;
            nextLetterInUpperCase = false;
            previousChar = currentChar;
        }

        return result.toString();
    }

}
