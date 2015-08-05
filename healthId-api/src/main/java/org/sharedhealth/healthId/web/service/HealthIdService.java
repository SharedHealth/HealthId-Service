package org.sharedhealth.healthId.web.service;

import org.sharedhealth.healthId.web.Model.MciHealthId;
import org.sharedhealth.healthId.web.config.HealthIdProperties;
import org.sharedhealth.healthId.web.repository.HealthIdRepository;
import org.sharedhealth.healthId.web.util.LuhnChecksumGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class HealthIdService {
    private final Pattern invalidHidPattern;
    private final HealthIdProperties healthIdProperties;
    private HealthIdRepository healthIdRepository;
    private LuhnChecksumGenerator checksumGenerator;

    @Autowired
    public HealthIdService(HealthIdProperties healthIdProperties, HealthIdRepository healthIdRepository, LuhnChecksumGenerator checksumGenerator) {
        this.healthIdProperties = healthIdProperties;
        this.healthIdRepository = healthIdRepository;
        this.checksumGenerator = checksumGenerator;
        invalidHidPattern = Pattern.compile(healthIdProperties.getInvalidHidPattern());
    }

    public long generate(long start, long end) {
        long numberOfValidHids = 0L;
        for (long i = start; i <= end; i++) {
            String possibleHid = String.valueOf(i);
            if (!invalidHidPattern.matcher(possibleHid).find()) {
                numberOfValidHids += 1;
                String newHealthId = possibleHid + checksumGenerator.generate(possibleHid.substring(1));
                healthIdRepository.saveHealthId(new MciHealthId(newHealthId));
            }
        }
        return numberOfValidHids;
    }

    public synchronized List<MciHealthId> getNextBlock() {
        return healthIdRepository.getNextBlock(healthIdProperties.getHealthIdBlockSize());
    }

    public synchronized List<MciHealthId> getNextBlock(int blockSize) {
        return healthIdRepository.getNextBlock(blockSize);
    }

    public void markUsed(MciHealthId nextMciHealthId) {
        healthIdRepository.removedUsedHid(nextMciHealthId);
    }
}