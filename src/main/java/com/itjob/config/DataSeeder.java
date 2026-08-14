package com.itjob.config;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.itjob.entities.Enums.Role;
import com.itjob.entities.SeedVersion;
import com.itjob.entities.Skills;
import com.itjob.entities.SystemConfig;
import com.itjob.entities.User;
import com.itjob.repository.SeedVersionRepo;
import com.itjob.repository.SkillsRepo;
import com.itjob.repository.SystemConfigRepo;
import com.itjob.repository.UserRepo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepo userRepo;
    private final SystemConfigRepo systemConfigRepo;
    private final SkillsRepo skillsRepo;
    private final SeedVersionRepo seedVersionRepo;

    // Bump this number whenever you change a default value below that must be pushed
    // to databases that are already seeded. On the next deploy, the seeder will
    // overwrite the existing system_config rows with the new values exactly once.
    private static final int SEED_VERSION = 2;

    @Override
    public void run(String... args) throws Exception {
        seedAdminUser();
        seedSystemConfigs();
        seedSkills();
    }

    private void seedAdminUser() {
        String adminEmail = "admin@admin.com";

        if (!userRepo.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setRole(Role.ADMIN);
            admin.setOtp("000000");
            admin.setExpiresAt(LocalDateTime.now().plusDays(365));
            admin.setIsUsed(false);
            admin.setProfileCompleted(true);

            userRepo.save(admin);

            System.out.println("========================================");
            System.out.println("  Admin user seeded successfully!");
            System.out.println("  Email: " + adminEmail);
            System.out.println("  Role: ADMIN");
            System.out.println("========================================");
        } else {
            System.out.println("Admin user already exists. Skipping seed.");
        }
    }

    private void seedSystemConfigs() {
        int seededVersion = getCurrentSeedVersion();
        // True when this deploy carries newer defaults than what the DB has seen.
        // Only then do we overwrite existing rows (exactly once per version bump).
        boolean applyDefaults = seededVersion < SEED_VERSION;

        // ==================== 1. General Settings ====================
        seedConfig("general_website_name", "IT Job Hunt", "text", "General", "Website display name", applyDefaults);
        seedConfig("general_company_name", "IT Job Hunt Pvt Ltd", "text", "General", "Legal company name", applyDefaults);
        seedConfig("general_support_email", "support@ithunt.com", "email", "General", "Customer support email address", applyDefaults);
        seedConfig("general_contact_number", "+91 9876543210", "text", "General", "Customer support phone number", applyDefaults);
        seedConfig("general_time_zone", "Asia/Kolkata", "text", "General", "System timezone", applyDefaults);

        // ==================== 2. Authentication & Security ====================
        seedConfig("auth_jwt_access_expiry", "1", "number", "Auth & Security", "JWT access token expiry in hours", applyDefaults);
        seedConfig("auth_jwt_refresh_expiry", "15", "number", "Auth & Security", "JWT refresh token expiry in days", applyDefaults);
        seedConfig("auth_otp_expiry_time", "5", "number", "Auth & Security", "OTP expiry time in minutes", applyDefaults);
        seedConfig("auth_max_login_attempts", "5", "number", "Auth & Security", "Maximum failed login attempts before lockout", applyDefaults);
        seedConfig("auth_enable_registration", "true", "toggle", "Auth & Security", "Allow new user registration", applyDefaults);

        // ==================== 3. Email (SMTP) Settings ====================
        // NOTE: smtp_password is NOT seeded here — it is read from application properties
        // (app.smtp.password) for security. These DB values act as a fallback only;
        // env vars (SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_SENDER_EMAIL) override them.
        // IMPORTANT: in the prod profile, host/port/username default to non-empty values
        // (smtp.sendgrid.net / 2525 / apikey), so the DB values below are only used when
        // the matching SMTP_* env var is also left unset. Set SMTP_PORT on Render if you
        // want a non-default port to take effect.
        // SendGrid SMTP: host smtp.sendgrid.net, username "apikey", port 2025 (this setup).
        seedConfig("smtp_host", "smtp.sendgrid.net", "text", "SMTP", "SMTP server hostname", applyDefaults);
        seedConfig("smtp_port", "2025", "number", "SMTP", "SMTP server port", applyDefaults);
        seedConfig("smtp_username", "apikey", "text", "SMTP", "SMTP authentication username", applyDefaults);
        seedConfig("smtp_sender_email", "test.harshal1@gmail.com", "email", "SMTP", "Email address shown in From field (must be verified in SendGrid)", applyDefaults);
        seedConfig("smtp_sender_name", "IT Job Hunt", "text", "SMTP", "Name shown in From field", applyDefaults);

        // ==================== 4. Cloud Storage ====================
        // NOTE: cloudinary_cloud_name, cloudinary_api_key, and cloudinary_api_secret are NOT seeded here.
        // They are read from application-dev.properties (app.cloudinary.*) for security.
        seedConfig("cloudinary_upload_folder", "itjob-portal/resumes", "text", "Cloud Storage", "Default upload folder in Cloudinary", applyDefaults);
        seedConfig("storage_max_resume_size", "10", "number", "Cloud Storage", "Maximum resume file size in MB", applyDefaults);
        seedConfig("storage_allowed_file_types", "pdf,doc,docx", "text", "Cloud Storage", "Comma-separated allowed file extensions", applyDefaults);

        // ==================== 5. Job Settings ====================
        seedConfig("job_max_per_recruiter", "50", "number", "Job Settings", "Maximum active jobs a recruiter can post", applyDefaults);
        seedConfig("job_expiry_days", "30", "number", "Job Settings", "Default job posting expiry in days", applyDefaults);
        seedConfig("job_auto_close_expired", "true", "toggle", "Job Settings", "Automatically close expired job postings", applyDefaults);
        seedConfig("job_featured_duration", "7", "number", "Job Settings", "Featured job duration in days", applyDefaults);
        seedConfig("job_require_admin_approval", "false", "toggle", "Job Settings", "Require admin approval before job goes live", applyDefaults);
        seedConfig("job_allow_duplicates", "false", "toggle", "Job Settings", "Allow recruiters to post duplicate job listings", applyDefaults);

        // ==================== 6. Candidate Settings ====================
        seedConfig("candidate_max_resume_size", "10", "number", "Candidate Settings", "Maximum resume upload size in MB", applyDefaults);
        seedConfig("candidate_allow_multiple_resumes", "false", "toggle", "Candidate Settings", "Allow candidates to upload multiple resumes", applyDefaults);
        seedConfig("candidate_profile_completion_required", "true", "toggle", "Candidate Settings", "Require profile completion before applying", applyDefaults);
        seedConfig("candidate_default_resume_visibility", "public", "select", "Candidate Settings", "Default resume visibility (public/private/contacts_only)", applyDefaults);

        // ==================== 7. Recruiter Settings ====================
        seedConfig("recruiter_company_verification_required", "true", "toggle", "Recruiter Settings", "Require company verification before posting jobs", applyDefaults);
        seedConfig("recruiter_approval_required", "false", "toggle", "Recruiter Settings", "Require admin approval for new recruiters", applyDefaults);
        seedConfig("recruiter_max_active_jobs", "50", "number", "Recruiter Settings", "Maximum active job postings per recruiter", applyDefaults);
        seedConfig("recruiter_allow_company_logo_upload", "true", "toggle", "Recruiter Settings", "Allow recruiters to upload company logo", applyDefaults);

        // ==================== 8. AI Settings ====================
        seedConfig("ai_auto_apply_enabled", "true", "toggle", "AI Settings", "Globally enable or disable AI Auto-Apply for all candidates", applyDefaults);

        if (applyDefaults) {
            saveSeedVersion(SEED_VERSION);
            System.out.println("System configurations seeded/updated to seed version " + SEED_VERSION);
        } else {
            System.out.println("System configurations already up to date (seed version " + SEED_VERSION + "). Skipping update.");
        }
    }

    private void seedSkills() {
        String[] defaultSkills = {
            // Java & JVM
            "Java", "Spring Boot", "Spring Security", "Spring Data JPA", "Spring Cloud",
            "Hibernate", "JPA", "JDBC", "JSP", "Servlets", "Thymeleaf",
            "Kotlin", "Groovy", "Gradle", "Maven",
            // Databases
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Elasticsearch",
            "Oracle", "SQL Server", "MariaDB", "Cassandra", "Firebase",
            // Web & Frontend
            "JavaScript", "TypeScript", "React", "Angular", "Vue.js",
            "HTML", "CSS", "SASS", "Tailwind CSS", "Bootstrap",
            "Next.js", "Node.js", "Express.js", "REST APIs", "GraphQL",
            // Cloud & DevOps
            "AWS", "Azure", "Google Cloud", "Docker", "Kubernetes",
            "Jenkins", "GitHub Actions", "GitLab CI", "Terraform", "Ansible",
            // Testing
            "JUnit", "Mockito", "Selenium", "Cypress", "Postman",
            // Other Languages
            "Python", "C", "C++", "C#", "Go", "Rust", "PHP", "Ruby",
            // Tools
            "Git", "Linux", "IntelliJ IDEA", "VS Code", "Eclipse",
            "Docker Compose", "Kafka", "RabbitMQ", "Nginx", "Apache"
        };

        int seeded = 0;
        for (String skillName : defaultSkills) {
            if (!skillsRepo.existsBySkillIgnoreCase(skillName)) {
                Skills skill = new Skills();
                skill.setSkill(skillName);
                skillsRepo.save(skill);
                seeded++;
            }
        }

        if (seeded > 0) {
            System.out.println("Seeded " + seeded + " default skills successfully!");
        } else {
            System.out.println("All default skills already exist. Skipping skill seed.");
        }
    }

    private void seedConfig(String key, String value, String inputType, String group, String description, boolean applyDefaults) {
        Optional<SystemConfig> existing = systemConfigRepo.findByConfigKey(key);
        if (existing.isPresent()) {
            // Key already exists. Only overwrite it when the seed version was bumped
            // (i.e. defaults in this file changed); manual admin edits are preserved
            // on every other startup.
            if (applyDefaults && !existing.get().getConfigValue().equals(value)) {
                existing.get().setConfigValue(value);
                existing.get().setUpdatedAt(LocalDateTime.now());
                systemConfigRepo.save(existing.get());
                System.out.println("  Updated config '" + key + "' to '" + value + "'");
            }
            return;
        }

        // Key not present yet — always insert (keeps fresh databases working).
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setInputType(inputType);
        config.setGroupName(group);
        config.setDescription(description);
        config.setUpdatedAt(LocalDateTime.now());
        systemConfigRepo.save(config);
    }

    private int getCurrentSeedVersion() {
        return seedVersionRepo.findAll().stream()
                .findFirst()
                .map(SeedVersion::getVersion)
                .orElse(0);
    }

    private void saveSeedVersion(int version) {
        SeedVersion seedVersion = seedVersionRepo.findAll().stream()
                .findFirst()
                .orElseGet(SeedVersion::new);
        seedVersion.setVersion(version);
        seedVersion.setUpdatedAt(LocalDateTime.now());
        seedVersionRepo.save(seedVersion);
    }
}
