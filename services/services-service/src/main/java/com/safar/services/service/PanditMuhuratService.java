package com.safar.services.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Server-side source of truth for Shubh Muhurat 2026 dates (Hindu Panchang,
 * indicative). Mirrors the web catalog so web + mobile + WhatsApp bot share one
 * dataset. Dates are location-independent and should be confirmed with a pandit
 * for the family's gotra/nakshatra and city.
 */
@Service
public class PanditMuhuratService {

    public record MuhuratEntry(String key, String label, String occasion, String note, List<String> dates) {}

    private static final List<MuhuratEntry> MUHURATS_2026 = List.of(
            new MuhuratEntry("grihaPravesh", "Griha Pravesh", "HOUSEWARMING",
                    "Chosen by moon nakshatra, tithi, weekday and lagna; avoids Chaturmas and eclipses. No muhurats in Jan, Aug, Sep, Oct 2026.",
                    List.of("2026-02-06","2026-02-11","2026-02-19","2026-02-21","2026-02-25","2026-03-04","2026-03-09","2026-03-13","2026-04-20","2026-05-08","2026-06-24","2026-07-01","2026-11-14","2026-11-20","2026-12-04","2026-12-11")),
            new MuhuratEntry("marriage", "Marriage / Vivah", "MARRIAGE",
                    "Fixed using the couple's nakshatras and rashis with an auspicious tithi and lagna. No dates in Jan or during Chaturmas (Jul-Oct).",
                    List.of("2026-02-04","2026-02-06","2026-02-08","2026-02-12","2026-02-19","2026-02-21","2026-02-25","2026-05-01","2026-05-05","2026-05-08","2026-05-13","2026-11-22","2026-11-25","2026-12-04","2026-12-11")),
            new MuhuratEntry("vehicle", "Vehicle Purchase", "CAR_VEHICLE",
                    "Favours swift nakshatras and Wed/Thu/Fri; Tue and Sat avoided. Akshaya Tritiya, Dussehra and Dhanteras are also auspicious. Avoid Rahu Kaal.",
                    List.of("2026-01-14","2026-01-21","2026-01-28","2026-02-11","2026-02-26","2026-03-06","2026-03-15","2026-04-13","2026-04-20","2026-05-11","2026-07-08","2026-12-13")),
            new MuhuratEntry("naamkaran", "Naamkaran", "BABY",
                    "Usually on the 11th-12th day after birth, so set from the baby's own birth nakshatra. Dates below are commonly cited 2026 windows.",
                    List.of("2026-01-14","2026-01-21","2026-01-28","2026-02-05","2026-02-12","2026-02-19","2026-03-06","2026-03-13","2026-04-09","2026-04-17","2026-08-13","2026-09-13","2026-10-14","2026-10-22")),
            new MuhuratEntry("bhoomiPujan", "Bhoomi Pujan", "HOUSEWARMING",
                    "Land worship before construction. Avoid Amavasya, eclipses, Holashtak and Pitru Paksha.",
                    List.of("2026-02-19","2026-03-04","2026-03-14","2026-04-20","2026-05-09","2026-06-24","2026-10-30","2026-11-14","2026-11-20","2026-12-04","2026-12-06","2026-12-11")),
            new MuhuratEntry("mundan", "Mundan", "BABY",
                    "Chudakarana (tonsure), daytime only, in odd years of the child's age. No muhurats Aug-Dec 2026.",
                    List.of("2026-01-20","2026-01-21","2026-01-31","2026-02-06","2026-02-11","2026-02-18","2026-02-26","2026-03-05","2026-03-16","2026-05-04","2026-05-09","2026-05-14","2026-06-17","2026-06-24","2026-07-02","2026-07-09","2026-07-15","2026-07-20"))
    );

    /** All categories, with each date list trimmed to dates on/after {@code from}. */
    public List<MuhuratEntry> upcoming(LocalDate from, String occasion) {
        String iso = from.toString();
        return MUHURATS_2026.stream()
                .filter(m -> occasion == null || occasion.isBlank() || m.occasion().equalsIgnoreCase(occasion))
                .map(m -> new MuhuratEntry(m.key(), m.label(), m.occasion(), m.note(),
                        m.dates().stream().filter(d -> d.compareTo(iso) >= 0).toList()))
                .toList();
    }
}
