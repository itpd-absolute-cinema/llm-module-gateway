package ru.inno.itpd.gateway.util;

import ru.inno.itpd.gateway.api.dto.Message;

import java.util.List;

/**
 * Грубая оценка числа токенов, когда апстрим не вернул usage: ~4 символа на токен для ASCII,
 * ~2 для остального (кириллица, CJK). Погрешность порядка ±30 %, поэтому события с такой оценкой
 * помечаются usage_source = "estimated".
 */
public final class TokenEstimator {
    private static final int PER_MESSAGE_OVERHEAD = 4;
    private static final int REPLY_PRIMING = 3;

    private TokenEstimator() {
    }

    public static int estimate(CharSequence text) {
        if (text == null || text.isEmpty()) return 0;
        double tokens = 0;
        String s = text.toString();
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            tokens += cp < 128 ? 0.25 : 0.5;
        }
        return (int) Math.ceil(tokens);
    }

    public static int estimatePrompt(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return 0;
        int total = REPLY_PRIMING;
        for (Message m : messages) {
            total += PER_MESSAGE_OVERHEAD + estimate(m.getContent());
        }
        return total;
    }
}
