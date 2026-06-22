package ru.iac.inscheck.util;

/**
 * Русский фонетический код ФИО («созвучное», п.3.1.4.3).
 *
 * Точный порт Oracle-функции a81.SoundexRUS из старой системы. В РС ЕРЗ колонки
 * IPERSON.META_FAM/META_IM/META_OT предрасчитаны именно этой функцией, поэтому
 * сравнение Metafon(вход) = IPerson.Meta_* в алгоритмах Р01/В01–В03/В06 работает
 * только при побайтовом совпадении кодов — реализация ниже воспроизводит
 * оригинал один-в-один.
 *
 * Алгоритм (оригинал):
 *   ZV_SOGL    = 'БЗДВГЖ'              — звонкие согласные
 *   GL_SOGL    = 'ПСТФКШ'              — их глухие пары (по позиции)
 *   SOGL_LIST  = 'БЗДВГЖПСТФКШЧЩХЦНМРЛ' — обрабатываемые согласные
 *   GLASN_LIST = 'АЯЕЁОИУЮЭЫ'          — гласные (для Ь/Ъ)
 * Для каждого символа:
 *   - согласная: оглушается (Б→П, З→С, Д→Т, В→Ф, Г→К, Ж→Ш); если предыдущий
 *     результат 'Т', а текущий 'С' — пара ТС схлопывается в 'Ц';
 *   - гласные группируются: А/О/Я→А, И/Е/Ё/Э/Й/Ы→И, У/Ю→У;
 *   - Ь/Ъ перед гласной → И, иначе отбрасывается;
 *   - прочие символы (пробелы, цифры, латиница, знаки) отбрасываются;
 *   - подряд идущие одинаковые символы результата схлопываются.
 * Пустой результат → '-'.
 */
public final class RussianMetaphone {

    private static final String ZV_SOGL = "БЗДВГЖ";
    private static final String GL_SOGL = "ПСТФКШ";
    private static final String SOGL_LIST = "БЗДВГЖПСТФКШЧЩХЦНМРЛ";
    private static final String GLASN_LIST = "АЯЕЁОИУЮЭЫ";

    private RussianMetaphone() {
    }

    /** Возвращает фонетический код, как Oracle a81.SoundexRUS (для пустого/null входа — "-"). */
    public static String encode(String input) {
        if (input == null) {
            return "-";
        }
        String fio = input.toUpperCase();
        StringBuilder res = new StringBuilder();

        for (int i = 0; i < fio.length(); i++) {
            char src = fio.charAt(i);
            Character cur = src;

            if (SOGL_LIST.indexOf(src) >= 0) {
                // согласная: оглушение по позиции в ZV_SOGL → GL_SOGL
                int zPos = ZV_SOGL.indexOf(src);
                if (zPos >= 0) {
                    cur = GL_SOGL.charAt(zPos);
                }
                // ТС → Ц (предыдущий символ результата 'Т', текущий 'С')
                if (res.length() > 0 && res.charAt(res.length() - 1) == 'Т' && cur == 'С') {
                    cur = 'Ц';
                    res.deleteCharAt(res.length() - 1);
                }
            } else if (src == 'А' || src == 'О' || src == 'Я') {
                cur = 'А';
            } else if (src == 'И' || src == 'Е' || src == 'Ё' || src == 'Э' || src == 'Й' || src == 'Ы') {
                cur = 'И';
            } else if (src == 'У' || src == 'Ю') {
                cur = 'У';
            } else if ((src == 'Ь' || src == 'Ъ')
                    && i + 1 < fio.length() && GLASN_LIST.indexOf(fio.charAt(i + 1)) >= 0) {
                cur = 'И';
            } else {
                cur = null; // отбрасываемый символ
            }

            // схлопывание подряд идущих одинаковых символов
            if (cur != null && (res.length() == 0 || res.charAt(res.length() - 1) != cur)) {
                res.append(cur.charValue());
            }
        }

        return res.length() == 0 ? "-" : res.toString();
    }
}
