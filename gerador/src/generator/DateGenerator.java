package generator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gerador de datas e horários realistas para transações financeiras.
 *
 * Características:
 * - O dia é selecionado uniformemente dentro do intervalo configurado [startDate, endDate].
 * - O horário do dia é modelado com uma mistura de distribuições:
 *   - 85% das transações seguem horários de pico comercial (mistura gaussiana bimodal diurna:
 *     pico do almoço ~13h e pico do final da tarde ~18h).
 *   - 15% das transações seguem distribuição uniforme nas 24h (para transações de internet,
 *     PIX e compras noturnas).
 * - Garante estritamente que os horários pertençam ao intervalo [00:00:00, 23:59:59]
 *   e que a data e hora final permaneça dentro de [startDateTime, endDateTime].
 */
public final class DateGenerator {

    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final LocalDate startDate;
    private final long totalDays;

    public DateGenerator(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            throw new IllegalArgumentException("As datas inicial e final não podem ser nulas.");
        }
        if (endDateTime.isBefore(startDateTime)) {
            throw new IllegalArgumentException("A data final deve ser posterior ou igual à data inicial.");
        }
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();
        this.totalDays = ChronoUnit.DAYS.between(startDate, endDate);
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    /**
     * Gera uma data e hora aleatória respeitando o comportamento realista de horários.
     */
    public LocalDateTime nextDateTime() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (true) {
            long dayOffset = totalDays > 0 ? random.nextLong(totalDays + 1) : 0;
            LocalDate day = startDate.plusDays(dayOffset);
            LocalTime time = nextRealisticTime(random);
            LocalDateTime dt = LocalDateTime.of(day, time);

            // Garante que respeita os limites caso o dia sorteado seja o dia inicial ou final
            if (!dt.isBefore(startDateTime) && !dt.isAfter(endDateTime)) {
                return dt;
            }
        }
    }

    /**
     * Gera um horário realista do dia em segundos [0, 86399].
     * Picos centrados em 13h (46800s) e 18h (64800s).
     */
    private LocalTime nextRealisticTime(ThreadLocalRandom random) {
        double roll = random.nextDouble();
        double secondOfDay;

        if (roll < 0.15) {
            // 15% transações distribuídas ao longo de todas as 24h
            secondOfDay = random.nextDouble(86400.0);
        } else if (roll < 0.55) {
            // 40% em torno do horário de almoço e comércio (centro 13:00, desvio de 2.5 horas)
            // 13h = 13 * 3600 = 46800s, desvio = 2.5 * 3600 = 9000s
            secondOfDay = 46800.0 + random.nextGaussian() * 9000.0;
        } else {
            // 45% em torno do fim de tarde e comércio noturno (centro 18:30, desvio de 2.5 horas)
            // 18.5h = 18.5 * 3600 = 66600s, desvio = 2.5 * 3600 = 9000s
            secondOfDay = 66600.0 + random.nextGaussian() * 9000.0;
        }

        // Truncamento seguro: se cair fora do dia (madrugada profunda ou pós-meia-noite),
        // reamostra ou projeta no intervalo [0, 86399]
        if (secondOfDay < 0.0) {
            secondOfDay = Math.abs(secondOfDay) % 86400.0;
        } else if (secondOfDay >= 86400.0) {
            secondOfDay = secondOfDay % 86400.0;
        }

        int totalSec = (int) secondOfDay;
        if (totalSec >= 86400) totalSec = 86399;
        return LocalTime.ofSecondOfDay(totalSec);
    }
}
