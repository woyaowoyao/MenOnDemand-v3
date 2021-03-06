package com.tojava.gateway.web.rest;

import com.tojava.gateway.RedisTestContainerExtension;
import com.tojava.gateway.VueGatewayv3App;
import com.tojava.gateway.domain.MentorSkill;
import com.tojava.gateway.repository.MentorSkillRepository;
import com.tojava.gateway.service.MentorSkillService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@link MentorSkillResource} REST controller.
 */
@SpringBootTest(classes = VueGatewayv3App.class)
@ExtendWith({ RedisTestContainerExtension.class, MockitoExtension.class })
@AutoConfigureMockMvc
@WithMockUser
public class MentorSkillResourceIT {

    private static final String DEFAULT_SELF_RATE = "AAAAAAAAAA";
    private static final String UPDATED_SELF_RATE = "BBBBBBBBBB";

    private static final Float DEFAULT_EXPERIENCE = 1F;
    private static final Float UPDATED_EXPERIENCE = 2F;

    @Autowired
    private MentorSkillRepository mentorSkillRepository;

    @Autowired
    private MentorSkillService mentorSkillService;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMentorSkillMockMvc;

    private MentorSkill mentorSkill;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MentorSkill createEntity(EntityManager em) {
        MentorSkill mentorSkill = new MentorSkill()
            .selfRate(DEFAULT_SELF_RATE)
            .experience(DEFAULT_EXPERIENCE);
        return mentorSkill;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MentorSkill createUpdatedEntity(EntityManager em) {
        MentorSkill mentorSkill = new MentorSkill()
            .selfRate(UPDATED_SELF_RATE)
            .experience(UPDATED_EXPERIENCE);
        return mentorSkill;
    }

    @BeforeEach
    public void initTest() {
        mentorSkill = createEntity(em);
    }

    @Test
    @Transactional
    public void createMentorSkill() throws Exception {
        int databaseSizeBeforeCreate = mentorSkillRepository.findAll().size();
        // Create the MentorSkill
        restMentorSkillMockMvc.perform(post("/api/mentor-skills")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(mentorSkill)))
            .andExpect(status().isCreated());

        // Validate the MentorSkill in the database
        List<MentorSkill> mentorSkillList = mentorSkillRepository.findAll();
        assertThat(mentorSkillList).hasSize(databaseSizeBeforeCreate + 1);
        MentorSkill testMentorSkill = mentorSkillList.get(mentorSkillList.size() - 1);
        assertThat(testMentorSkill.getSelfRate()).isEqualTo(DEFAULT_SELF_RATE);
        assertThat(testMentorSkill.getExperience()).isEqualTo(DEFAULT_EXPERIENCE);
    }

    @Test
    @Transactional
    public void createMentorSkillWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = mentorSkillRepository.findAll().size();

        // Create the MentorSkill with an existing ID
        mentorSkill.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restMentorSkillMockMvc.perform(post("/api/mentor-skills")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(mentorSkill)))
            .andExpect(status().isBadRequest());

        // Validate the MentorSkill in the database
        List<MentorSkill> mentorSkillList = mentorSkillRepository.findAll();
        assertThat(mentorSkillList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void getAllMentorSkills() throws Exception {
        // Initialize the database
        mentorSkillRepository.saveAndFlush(mentorSkill);

        // Get all the mentorSkillList
        restMentorSkillMockMvc.perform(get("/api/mentor-skills?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(mentorSkill.getId().intValue())))
            .andExpect(jsonPath("$.[*].selfRate").value(hasItem(DEFAULT_SELF_RATE)))
            .andExpect(jsonPath("$.[*].experience").value(hasItem(DEFAULT_EXPERIENCE.doubleValue())));
    }
    
    @Test
    @Transactional
    public void getMentorSkill() throws Exception {
        // Initialize the database
        mentorSkillRepository.saveAndFlush(mentorSkill);

        // Get the mentorSkill
        restMentorSkillMockMvc.perform(get("/api/mentor-skills/{id}", mentorSkill.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(mentorSkill.getId().intValue()))
            .andExpect(jsonPath("$.selfRate").value(DEFAULT_SELF_RATE))
            .andExpect(jsonPath("$.experience").value(DEFAULT_EXPERIENCE.doubleValue()));
    }
    @Test
    @Transactional
    public void getNonExistingMentorSkill() throws Exception {
        // Get the mentorSkill
        restMentorSkillMockMvc.perform(get("/api/mentor-skills/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateMentorSkill() throws Exception {
        // Initialize the database
        mentorSkillService.save(mentorSkill);

        int databaseSizeBeforeUpdate = mentorSkillRepository.findAll().size();

        // Update the mentorSkill
        MentorSkill updatedMentorSkill = mentorSkillRepository.findById(mentorSkill.getId()).get();
        // Disconnect from session so that the updates on updatedMentorSkill are not directly saved in db
        em.detach(updatedMentorSkill);
        updatedMentorSkill
            .selfRate(UPDATED_SELF_RATE)
            .experience(UPDATED_EXPERIENCE);

        restMentorSkillMockMvc.perform(put("/api/mentor-skills")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(updatedMentorSkill)))
            .andExpect(status().isOk());

        // Validate the MentorSkill in the database
        List<MentorSkill> mentorSkillList = mentorSkillRepository.findAll();
        assertThat(mentorSkillList).hasSize(databaseSizeBeforeUpdate);
        MentorSkill testMentorSkill = mentorSkillList.get(mentorSkillList.size() - 1);
        assertThat(testMentorSkill.getSelfRate()).isEqualTo(UPDATED_SELF_RATE);
        assertThat(testMentorSkill.getExperience()).isEqualTo(UPDATED_EXPERIENCE);
    }

    @Test
    @Transactional
    public void updateNonExistingMentorSkill() throws Exception {
        int databaseSizeBeforeUpdate = mentorSkillRepository.findAll().size();

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMentorSkillMockMvc.perform(put("/api/mentor-skills")
            .contentType(MediaType.APPLICATION_JSON)
            .content(TestUtil.convertObjectToJsonBytes(mentorSkill)))
            .andExpect(status().isBadRequest());

        // Validate the MentorSkill in the database
        List<MentorSkill> mentorSkillList = mentorSkillRepository.findAll();
        assertThat(mentorSkillList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteMentorSkill() throws Exception {
        // Initialize the database
        mentorSkillService.save(mentorSkill);

        int databaseSizeBeforeDelete = mentorSkillRepository.findAll().size();

        // Delete the mentorSkill
        restMentorSkillMockMvc.perform(delete("/api/mentor-skills/{id}", mentorSkill.getId())
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<MentorSkill> mentorSkillList = mentorSkillRepository.findAll();
        assertThat(mentorSkillList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
