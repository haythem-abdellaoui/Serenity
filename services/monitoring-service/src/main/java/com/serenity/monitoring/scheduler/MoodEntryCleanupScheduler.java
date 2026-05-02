package com.serenity.monitoring.scheduler;

import com.serenity.monitoring.repository.MoodEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class MoodEntryCleanupScheduler {

	private final MoodEntryRepository moodEntryRepository;

	@Value("${app.cleanup.retention-days:90}")
	private long retentionDays;

	@Scheduled(cron = "${app.cleanup.cron:0 0 3 * * ?}", zone = "${app.cleanup.timezone:Africa/Tunis}")
	@Transactional
	public void deleteOldMoodEntriesWithoutClinicalTriggers() {
		Date cutoff = Date.from(Instant.now().minus(retentionDays, ChronoUnit.DAYS));

		long candidates = moodEntryRepository.countOldEntriesWithoutClinicalTriggers(cutoff);
		if (candidates == 0) {
			log.debug("Cleanup scheduler: no old mood entries to delete (cutoff={})", cutoff);
			return;
		}

		int deleted = moodEntryRepository.deleteOldEntriesWithoutClinicalTriggers(cutoff);
		log.info("Cleanup scheduler deleted {} mood entries older than {} days (candidates={}, cutoff={})",
				deleted, retentionDays, candidates, cutoff);
	}
}

