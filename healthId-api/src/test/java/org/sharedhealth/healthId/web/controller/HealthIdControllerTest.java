package org.sharedhealth.healthId.web.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.sharedhealth.healthId.web.Model.GeneratedHidRange;
import org.sharedhealth.healthId.web.Model.MciHealthId;
import org.sharedhealth.healthId.web.config.HealthIdProperties;
import org.sharedhealth.healthId.web.security.UserInfo;
import org.sharedhealth.healthId.web.security.UserProfile;
import org.sharedhealth.healthId.web.service.GeneratedHidRangeService;
import org.sharedhealth.healthId.web.service.HealthIdService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class HealthIdControllerTest {
    @Mock
    HealthIdService healthIdService;

    @Mock
    GeneratedHidRangeService generatedHidRangeService;

    HealthIdProperties properties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(getUserInfo(), null));
        properties = new HealthIdProperties();
        properties.setInvalidHidPattern("^[^9]|^.[^89]|(^\\d{0,9}$)|(^\\d{11,}$)|((\\d)\\4{2})\\d*((\\d)\\6{2})|(\\d)\\7{3}");
        properties.setMciStartHid("9800000000");
        properties.setMciEndHid("9999999999");

    }

    private UserInfo getUserInfo() {
        UserProfile userProfile = new UserProfile("facility", "100067", null);

        return new UserInfo("102", "ABC", "abc@mail", 1, true, "111100",
                new ArrayList<String>(), asList(userProfile));
    }

    @Test
    public void testGenerate() {
        long start = properties.getMciStartHid(), end = properties.getMciEndHid();
        when(healthIdService.generate(start, end)).thenReturn(100L);
        HealthIdController healthIdController = new HealthIdController(healthIdService, generatedHidRangeService, properties);
        assertEquals("GENERATED 100 Ids", healthIdController.generate().getResult());
        verify(healthIdService, times(1)).generate(start, end);
    }

    @Test
    public void testGetNextBlock() {
        when(healthIdService.getNextBlock()).thenReturn(getNextBlock());
        HealthIdController healthIdController = new HealthIdController(healthIdService, generatedHidRangeService, properties);
        assertEquals(3, healthIdController.nextBlock().size());
        verify(healthIdService, times(1)).getNextBlock();
    }

    private ArrayList<MciHealthId> getNextBlock() {
        ArrayList<MciHealthId> MciHealthIds = new ArrayList<>();
        MciHealthIds.add(new MciHealthId("123"));
        MciHealthIds.add(new MciHealthId("124"));
        MciHealthIds.add(new MciHealthId("125"));
        return MciHealthIds;
    }

    @Test
    public void testGenerateRange() {
        long start = 9800100100L, end = 9800100200L;
        when(healthIdService.generate(start, end)).thenReturn(100L);
        HealthIdController healthIdController = new HealthIdController(healthIdService, generatedHidRangeService, properties);
        assertEquals("GENERATED 100 Ids", healthIdController.generateRange(start, end).getResult());
        verify(healthIdService, times(1)).generate(start, end);
    }

    @Test
    public void shouldNotGenerateOverlappingHids() throws Exception {
        long start = 9800100100L, end = 9800100200L;
        when(healthIdService.generate(start, end)).thenReturn(100L);
        HealthIdController healthIdController = new HealthIdController(healthIdService, generatedHidRangeService, properties);
        assertEquals("GENERATED 100 Ids", healthIdController.generateRange(start, end).getResult());
        verify(healthIdService, times(1)).generate(start, end);
        when(generatedHidRangeService.getPreGeneratedHidRanges()).thenReturn(asList(new GeneratedHidRange(start, end)));
        assertEquals("Range overlaps with pregenerated healthIds", healthIdController.generateRange(start, end).getResult());
        assertEquals("Range overlaps with pregenerated healthIds", healthIdController.generateRange(start - 1, end + 1).getResult());
        assertEquals("Range overlaps with pregenerated healthIds", healthIdController.generateRange(end, 9800100202L).getResult());
        assertEquals("Range overlaps with pregenerated healthIds", healthIdController.generateRange(9800100000L, start).getResult());
    }
}