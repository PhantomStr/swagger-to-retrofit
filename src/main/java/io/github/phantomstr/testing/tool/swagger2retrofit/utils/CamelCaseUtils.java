package io.github.phantomstr.testing.tool.swagger2retrofit.utils;

public class CamelCaseUtils {

    public static String toCamelCase(String string, boolean firstWordToLowerCase) {
        char currentChar, previousChar = '\u0000'; // Текущий и предыдущий символ прохода
        StringBuilder result = new StringBuilder(); // Результат функции в виде строкового билдера

        boolean firstLetterArrived = !firstWordToLowerCase; // Флаг, отвечающий за написание первого символа результата в lowercase
        boolean nextLetterInUpperCase = true; // Флаг, приказывающий следующий добавляемый символ писать в UPPERCASE

        // Проходимся по всем символам полученной строки
        for (int i = 0; i < string.length(); i++) {
            currentChar = string.charAt(i);

			/* Если текущий символ не цифробуква -
				приказываем следующий символ писать Большим (начать новое слово) и идем на следующую итерацию.
			   Если предыдущий символ это маленькая буква или цифра, а текущий это большая буква -
			    приказываем текущий символ писать Большим (начать новое слово).
			*/
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

            // Если приказано писать Большую букву, и первая буква уже написана.
            if (nextLetterInUpperCase && firstLetterArrived) {
                result.append(Character.toUpperCase(currentChar));
            } else {
                result.append(Character.toLowerCase(currentChar));
            }

            // Устанавливаем флаги.
            firstLetterArrived = true;
            nextLetterInUpperCase = false;
            previousChar = currentChar;
        }

        // Возвращаем полученный результат.
        return result.toString();
    }

}
