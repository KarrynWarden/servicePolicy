package ru.iac.inscheck.util;

/**
 * Русский метафон («созвучное», п.3.1.4.3) для приведения ФИО к фонетическому коду.
 *
 * В старой системе метафон-коды хранились в IPERSON в колонках META_FAM/META_IM/
 * META_OT (на это прямо ссылается постановка, п.4.2.5). Сравнение идёт по равенству
 * Metafon(вход) = IPerson.Meta_*. Поэтому здесь считается метафон ВХОДНОГО значения,
 * а в БД сравнивается с предрасчитанными колонками.
 *
 * TODO: алгоритм должен совпадать с генератором META_* в регистре РС ЕРЗ.
 * Реализация ниже — классический русский metaphone (оглушение, схлопывание
 * гласных, замена созвучных согласных). При расхождении с регистром — поправить
 * здесь и/или пересчитать колонки META_* одним и тем же алгоритмом.
 */
public final class RussianMetaphone {

    private RussianMetaphone() {
    }

    public static String encode(String input) {
        if (input == null) {
            return null;
        }
        String s = input.trim().toUpperCase();
        if (s.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(s.length());

        // 1. Группы гласных -> к одной «опорной» гласной (О-А-Э-И).
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 'Й', 'Ё', 'Ю', 'Я', 'Е' -> sb.append("И");
                case 'О', 'Ы' -> sb.append("А");
                case 'У' -> sb.append("О");
                case 'Э' -> sb.append("И");
                default -> sb.append(c);
            }
        }

        // 2. Оглушение звонких согласных на конце и перед глухими.
        String t = sb.toString();
        StringBuilder out = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            char next = i + 1 < t.length() ? t.charAt(i + 1) : 0;
            boolean endOrVoiceless = (next == 0) || isVoiceless(next);
            char repl = switch (c) {
                case 'Б' -> endOrVoiceless ? 'П' : 'Б';
                case 'З' -> endOrVoiceless ? 'С' : 'З';
                case 'Д' -> endOrVoiceless ? 'Т' : 'Д';
                case 'В' -> endOrVoiceless ? 'Ф' : 'В';
                case 'Г' -> endOrVoiceless ? 'К' : 'Г';
                default -> c;
            };
            out.append(repl);
        }

        // 3. Удаление мягкого/твёрдого знаков и схлопывание дублей.
        StringBuilder res = new StringBuilder(out.length());
        char prev = 0;
        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);
            if (c == 'Ь' || c == 'Ъ') {
                continue;
            }
            if (c != prev) {
                res.append(c);
            }
            prev = c;
        }

        return res.toString();
    }

    private static boolean isVoiceless(char c) {
        return "ПФКТШСХЦЧЩ".indexOf(c) >= 0;
    }
}
