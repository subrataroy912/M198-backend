package com.M198.Majorproject.common.seed;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.M198.Majorproject.core.course.entity.Course;
import com.M198.Majorproject.core.course.entity.CourseAccessType;
import com.M198.Majorproject.core.course.entity.CourseMembership;
import com.M198.Majorproject.core.course.entity.CourseStatus;
import com.M198.Majorproject.core.course.entity.CourseVisibility;
import com.M198.Majorproject.core.course.entity.Coursework;
import com.M198.Majorproject.core.course.entity.CourseworkStatus;
import com.M198.Majorproject.core.course.entity.CourseworkType;
import com.M198.Majorproject.core.course.entity.MembershipRole;
import com.M198.Majorproject.core.course.entity.MembershipStatus;
import com.M198.Majorproject.core.course.entity.StudentGradebookEntry;
import com.M198.Majorproject.core.course.entity.Submission;
import com.M198.Majorproject.core.course.entity.SubmissionStatus;
import com.M198.Majorproject.core.course.repository.CourseMembershipRepository;
import com.M198.Majorproject.core.course.repository.CourseRepository;
import com.M198.Majorproject.core.course.repository.CourseworkRepository;
import com.M198.Majorproject.core.course.repository.StudentGradebookEntryRepository;
import com.M198.Majorproject.core.course.repository.SubmissionRepository;
import com.M198.Majorproject.discovery.comment.entity.Comment;
import com.M198.Majorproject.discovery.comment.entity.CommentTargetType;
import com.M198.Majorproject.discovery.comment.entity.CommentVisibility;
import com.M198.Majorproject.discovery.comment.repository.CommentRepository;
import com.M198.Majorproject.discovery.explore.entity.CourseDiscovery;
import com.M198.Majorproject.discovery.explore.repository.CourseDiscoveryRepository;
import com.M198.Majorproject.discovery.notification.entity.Notification;
import com.M198.Majorproject.discovery.notification.entity.NotificationResourceType;
import com.M198.Majorproject.discovery.notification.entity.NotificationSettings;
import com.M198.Majorproject.discovery.notification.entity.NotificationType;
import com.M198.Majorproject.discovery.notification.repository.NotificationRepository;
import com.M198.Majorproject.discovery.notification.repository.NotificationSettingsRepository;
import com.M198.Majorproject.user.identity.entity.AccountStatus;
import com.M198.Majorproject.user.identity.entity.User;
import com.M198.Majorproject.user.identity.repository.UserRepository;
import com.M198.Majorproject.user.profile.entity.ProfileVisibility;
import com.M198.Majorproject.user.profile.entity.UserProfile;
import com.M198.Majorproject.user.profile.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DataSeedRunner
 *
 * Populates MongoDB with localized academic entities reflecting North Bengal (Siliguri, Jalpaiguri,
 * Darjeeling, Dooars) institutions including JGEC, SIT, NBU, Siliguri Govt Polytechnic, and Jalpaiguri Polytechnic.
 *
 * Activated safely via app property: app.seed.enabled=true
 * Unified password for all seed personas: Password123!
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class DataSeedRunner implements CommandLineRunner {

    private static final String UNIFIED_PASSWORD = "Password123!";
    private static final List<String> SEED_DOMAINS = List.of(
            "@jgec.ac.in",
            "@sit.ac.in",
            "@nbu.ac.in",
            "@sgpolytechnic.ac.in",
            "@jpicampus.ac.in");

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final CourseRepository courseRepository;
    private final CourseDiscoveryRepository courseDiscoveryRepository;
    private final CourseMembershipRepository courseMembershipRepository;
    private final CourseworkRepository courseworkRepository;
    private final SubmissionRepository submissionRepository;
    private final StudentGradebookEntryRepository studentGradebookEntryRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("==================================================================");
        log.info(" Starting North Bengal / India Academic Bulk Data Seeder... ");
        log.info("==================================================================");

        cleanPreviousSeedData();

        String passwordHash = passwordEncoder.encode(UNIFIED_PASSWORD);
        Instant now = Instant.now();

        // 1. Seed Personas (Users & Profiles)
        Map<String, User> userMap = seedUsers(passwordHash, now);
        seedProfiles(userMap, now);
        seedNotificationSettings(userMap, now);

        // 2. Seed Academic Spaces (Courses & Discovery)
        Map<String, Course> courseMap = seedCourses(userMap, now);

        // 3. Seed Course Memberships with realistic overlaps
        seedMemberships(userMap, courseMap, now);

        // 4. Seed Coursework, Submissions, Gradebook entries, Comments & Notifications
        seedActivities(userMap, courseMap, now);

        log.info("==================================================================");
        log.info(" North Bengal Data Seeding Complete!");
        log.info(" Total Seed Users: {}", userMap.size());
        log.info(" Total Seed Courses: {}", courseMap.size());
        log.info(" All seed accounts can log in using password: {}", UNIFIED_PASSWORD);
        log.info("==================================================================");
    }

    private void cleanPreviousSeedData() {
        log.info("Cleaning previously seeded institutional data...");

        List<User> existingSeedUsers = userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null && SEED_DOMAINS.stream().anyMatch(d -> u.getEmail().endsWith(d)))
                .toList();

        if (existingSeedUsers.isEmpty()) {
            log.info("No prior seed data detected.");
            return;
        }

        Set<String> seedUserIds = new HashSet<>();
        for (User u : existingSeedUsers) {
            seedUserIds.add(u.getId());
        }

        // Find courses owned by seed users
        List<Course> seedCourses = courseRepository.findAll().stream()
                .filter(c -> seedUserIds.contains(c.getOwnerId()))
                .toList();

        for (Course c : seedCourses) {
            String courseId = c.getId();
            courseworkRepository.deleteAllByCourseId(courseId);
            submissionRepository.deleteAllByCourseId(courseId);
            studentGradebookEntryRepository.deleteAllByCourseId(courseId);
            commentRepository.deleteAllByCourseId(courseId);
            courseMembershipRepository.deleteAllByCourseId(courseId);
            courseDiscoveryRepository.deleteByCourseId(courseId);
            courseRepository.deleteById(courseId);
        }

        for (String userId : seedUserIds) {
            userProfileRepository.findByUserId(userId).ifPresent(p -> userProfileRepository.deleteById(p.getId()));
            notificationSettingsRepository.findByUserId(userId)
                    .ifPresent(ns -> notificationSettingsRepository.deleteById(ns.getId()));
            notificationRepository.deleteAll(
                    notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId, null).getContent());
            userRepository.deleteById(userId);
        }

        log.info("Prior seed records safely cleared.");
    }

    private Map<String, User> seedUsers(String passwordHash, Instant now) {
        log.info("Seeding user accounts...");

        List<SeedUserDefinition> defs = List.of(
                // Faculty / Creators
                new SeedUserDefinition("debasmita.ghosh@jgec.ac.in", true, false),
                new SeedUserDefinition("sourav.sen@sit.ac.in", true, false),
                new SeedUserDefinition("subir.tea@nbu.ac.in", true, false),
                new SeedUserDefinition("arindam.it@jgec.ac.in", true, false),
                new SeedUserDefinition("amitesh.c@sgpolytechnic.ac.in", true, false),
                // Learners / Members
                new SeedUserDefinition("ananya.m@jgec.ac.in", false, false),
                new SeedUserDefinition("rohan.das@sit.ac.in", false, false),
                new SeedUserDefinition("sneha.roy@nbu.ac.in", false, false),
                new SeedUserDefinition("bikram.m@jgec.ac.in", false, false),
                new SeedUserDefinition("priyanka.p@sit.ac.in", false, false),
                new SeedUserDefinition("amitava.b@jgec.ac.in", false, false),
                new SeedUserDefinition("tanmoy.b@sgpolytechnic.ac.in", false, false),
                new SeedUserDefinition("subhamita.r@jpicampus.ac.in", false, false),
                new SeedUserDefinition("tathagata.d@sit.ac.in", false, false),
                new SeedUserDefinition("riya.s@jgec.ac.in", false, false),
                new SeedUserDefinition("debjit.k@nbu.ac.in", false, false),
                new SeedUserDefinition("pooja.c@sit.ac.in", false, false));

        Map<String, User> map = new HashMap<>();
        for (SeedUserDefinition def : defs) {
            User user = User.builder()
                    .email(def.email)
                    .passwordHash(passwordHash)
                    .status(AccountStatus.ACTIVE)
                    .active(true)
                    .verified(true)
                    .canCreateCourses(def.canCreateCourses)
                    .isAdmin(def.isAdmin)
                    .lastLoginAt(now.minus(Duration.ofHours(3)))
                    .build();
            user = userRepository.save(user);
            map.put(def.email, user);
        }
        return map;
    }

    private void seedProfiles(Map<String, User> userMap, Instant now) {
        log.info("Seeding localized public profiles with North Bengal contexts...");

        List<SeedProfileDefinition> profiles = List.of(
                // 1. Debasmita Ghosh
                new SeedProfileDefinition(
                        "debasmita.ghosh@jgec.ac.in", "debasmita_cse", "Debasmita", "Ghosh",
                        "Prof. Debasmita Ghosh", "Computer Science and Engineering",
                        "Professor & Head of Department, Computer Science & Engineering at Jalpaiguri Government Engineering College (JGEC). Research in Algorithms and Graph Systems.",
                        "Siliguri", "India", "Female", true),
                // 2. Sourav Sen
                new SeedProfileDefinition(
                        "sourav.sen@sit.ac.in", "sourav_ece", "Sourav", "Sen",
                        "Dr. Sourav Sen", "Electronics and Communication Engineering",
                        "Associate Professor of ECE at Siliguri Institute of Technology (SIT). Focused on IoT, Embedded Micro-controllers, and Smart City Telemetry.",
                        "Siliguri", "India", "Male", true),
                // 3. Subir Roy Chowdhury
                new SeedProfileDefinition(
                        "subir.tea@nbu.ac.in", "subir_teascience", "Subir", "Roy Chowdhury",
                        "Dr. Subir Roy Chowdhury", "Tea Science and Management",
                        "Senior Faculty at University of North Bengal (NBU), Raja Rammohunpur. Specializing in Dooars tea bush agronomy, plantation management, and quality tasting.",
                        "Jalpaiguri", "India", "Male", true),
                // 4. Arindam Paul
                new SeedProfileDefinition(
                        "arindam.it@jgec.ac.in", "arindam_it", "Arindam", "Paul",
                        "Prof. Arindam Paul", "Information Technology",
                        "Assistant Professor in Information Technology at JGEC. Leading coursework in Cloud Distributed Systems and Kubernetes DevOps.",
                        "Siliguri", "India", "Male", true),
                // 5. Amitesh Chakraborty
                new SeedProfileDefinition(
                        "amitesh.c@sgpolytechnic.ac.in", "amitesh_sgp", "Amitesh", "Chakraborty",
                        "Er. Amitesh Chakraborty", "Computer Science and Engineering",
                        "Senior Lecturer in Computer Science & Technology at Siliguri Government Polytechnic, Dabgram. Teaching Microprocessors and System Architecture.",
                        "Siliguri", "India", "Male", true),
                // 6. Ananya Mukherjee
                new SeedProfileDefinition(
                        "ananya.m@jgec.ac.in", "ananya_jgec", "Ananya", "Mukherjee",
                        "Ananya Mukherjee", "Computer Science and Engineering",
                        "Final year CSE undergraduate at JGEC. Competitive programmer, open-source enthusiast, and algorithms tutor.",
                        "Jalpaiguri", "India", "Female", false),
                // 7. Rohan Das
                new SeedProfileDefinition(
                        "rohan.das@sit.ac.in", "rohan_sit", "Rohan", "Das",
                        "Rohan Das", "Computer Science and Engineering",
                        "3rd Year CSE student at Siliguri Institute of Technology. Exploring Deep Learning, Neural Networks, and OpenCV.",
                        "Siliguri", "India", "Male", false),
                // 8. Sneha Roy
                new SeedProfileDefinition(
                        "sneha.roy@nbu.ac.in", "sneha_tea", "Sneha", "Roy",
                        "Sneha Roy", "Tea Science and Management",
                        "M.Sc Scholar in Tea Science at North Bengal University. Researching bio-pesticides and organic plucking practices in Darjeeling tea gardens.",
                        "Jalpaiguri", "India", "Female", false),
                // 9. Bikram Mondal
                new SeedProfileDefinition(
                        "bikram.m@jgec.ac.in", "bikram_ece", "Bikram", "Mondal",
                        "Bikram Mondal", "Electronics and Communication Engineering",
                        "3rd Year ECE student at JGEC. Hardware tinkerer, PCB designer, and embedded systems programmer.",
                        "Cooch Behar", "India", "Male", false),
                // 10. Priyanka Paul
                new SeedProfileDefinition(
                        "priyanka.p@sit.ac.in", "priyanka_it", "Priyanka", "Paul",
                        "Priyanka Paul", "Information Technology",
                        "2nd Year IT student at SIT Siliguri. Full-stack developer building cloud-native web applications.",
                        "Siliguri", "India", "Female", false),
                // 11. Amitava Bannerjee
                new SeedProfileDefinition(
                        "amitava.b@jgec.ac.in", "amitava_civil", "Amitava", "Bannerjee",
                        "Amitava Bannerjee", "Civil Engineering",
                        "4th Year Civil Engineering student at JGEC. Researching slope stability and landslide hazard mapping in Darjeeling hills.",
                        "Darjeeling", "India", "Male", false),
                // 12. Tanmoy Barman
                new SeedProfileDefinition(
                        "tanmoy.b@sgpolytechnic.ac.in", "tanmoy_cst", "Tanmoy", "Barman",
                        "Tanmoy Barman", "Computer Science and Engineering",
                        "2nd Year Diploma CST scholar at Siliguri Government Polytechnic, Dabgram. Assembly enthusiast.",
                        "Siliguri", "India", "Male", false),
                // 13. Subhamita Roy
                new SeedProfileDefinition(
                        "subhamita.r@jpicampus.ac.in", "subhamita_civil", "Subhamita", "Roy",
                        "Subhamita Roy", "Civil Engineering",
                        "3rd Year Diploma Civil student at Jalpaiguri Polytechnic Institute, Danguajhar. Interested in building surveying and AutoCAD.",
                        "Jalpaiguri", "India", "Female", false),
                // 14. Tathagata Das
                new SeedProfileDefinition(
                        "tathagata.d@sit.ac.in", "tathagata_cse", "Tathagata", "Das",
                        "Tathagata Das", "Computer Science and Engineering",
                        "3rd Year CSE student at SIT. Building real-time interactive educational tools.",
                        "Siliguri", "India", "Male", false),
                // 15. Riya Sarkar
                new SeedProfileDefinition(
                        "riya.s@jgec.ac.in", "riya_cse", "Riya", "Sarkar",
                        "Riya Sarkar", "Computer Science and Engineering",
                        "Final year CSE scholar at JGEC. Cloud computing and database enthusiast.",
                        "Jalpaiguri", "India", "Female", false),
                // 16. Debjit Kar
                new SeedProfileDefinition(
                        "debjit.k@nbu.ac.in", "debjit_nbu", "Debjit", "Kar",
                        "Debjit Kar", "Tea Science and Management",
                        "Research Scholar at University of North Bengal, studying soil microbiome dynamics in Terai agro-ecosystems.",
                        "Siliguri", "India", "Male", false),
                // 17. Pooja Chhetri
                new SeedProfileDefinition(
                        "pooja.c@sit.ac.in", "pooja_ece", "Pooja", "Chhetri",
                        "Pooja Chhetri", "Electronics and Communication Engineering",
                        "3rd Year ECE student at SIT. Working on remote sensing and environmental monitoring in the Himalayan foothills.",
                        "Darjeeling", "India", "Female", false));

        for (SeedProfileDefinition p : profiles) {
            User user = userMap.get(p.email);
            if (user == null) continue;

            UserProfile profile = UserProfile.builder()
                    .userId(user.getId())
                    .handle(p.handle)
                    .firstName(p.firstName)
                    .lastName(p.lastName)
                    .displayName(p.displayName)
                    .headline(p.headline)
                    .about(p.about)
                    .city(p.city)
                    .country(p.country)
                    .gender(p.gender)
                    .profileVisibility(ProfileVisibility.PUBLIC)
                    .canCreateCourses(p.canCreateCourses)
                    .profileCompleted(true)
                    .createdAt(now.minus(Duration.ofDays(30)))
                    .updatedAt(now.minus(Duration.ofDays(2)))
                    .build();

            userProfileRepository.save(profile);
        }
    }

    private void seedNotificationSettings(Map<String, User> userMap, Instant now) {
        for (User user : userMap.values()) {
            NotificationSettings settings = NotificationSettings.builder()
                    .userId(user.getId())
                    .emailEnabled(true)
                    .pushEnabled(true)
                    .inAppEnabled(true)
                    .createdAt(now.minus(Duration.ofDays(30)))
                    .updatedAt(now)
                    .build();
            notificationSettingsRepository.save(settings);
        }
    }

    private Map<String, Course> seedCourses(Map<String, User> userMap, Instant now) {
        log.info("Seeding North Bengal university & polytechnic spaces...");

        User debasmita = userMap.get("debasmita.ghosh@jgec.ac.in");
        User sourav = userMap.get("sourav.sen@sit.ac.in");
        User subir = userMap.get("subir.tea@nbu.ac.in");
        User arindam = userMap.get("arindam.it@jgec.ac.in");
        User amitesh = userMap.get("amitesh.c@sgpolytechnic.ac.in");

        List<SeedCourseDefinition> courseDefs = List.of(
                // 1. JGEC CSE - DSA Lab
                new SeedCourseDefinition(
                        "c_jgec_dsa",
                        debasmita.getId(),
                        "Data Structures & Algorithms Laboratory",
                        "3rd Sem CSE - Sec A",
                        "Computer Science and Engineering",
                        "Rigorous algorithmic problem solving, graph representations, and complexity optimization for CSE undergrads at Jalpaiguri Government Engineering College.",
                        "Lab 3, CSE Building, JGEC Jalpaiguri",
                        "theme-indigo",
                        List.of("algorithms", "jgec", "datastructures", "jalpaiguri", "cse")),
                // 2. SIT CSE/IT - Machine Learning
                new SeedCourseDefinition(
                        "c_sit_ml",
                        debasmita.getId(),
                        "Machine Learning & Pattern Recognition",
                        "7th Sem CSE/IT Elective",
                        "Computer Science and Engineering",
                        "Advanced predictive modeling, statistical learning, and neural network foundations at Siliguri Institute of Technology.",
                        "SIT Smart Room 204, Siliguri",
                        "theme-purple",
                        List.of("machine-learning", "sit", "siliguri", "python", "ai")),
                // 3. SIT ECE - IoT & Embedded Systems
                new SeedCourseDefinition(
                        "c_sit_iot",
                        sourav.getId(),
                        "Embedded Systems & IoT for Smart Siliguri",
                        "5th Sem ECE",
                        "Electronics and Communication Engineering",
                        "Designing microcontroller sensor nodes for smart traffic, flood prediction, and climate monitoring in the Siliguri-Jalpaiguri urban corridor.",
                        "IoT Hardware Lab, SIT Sukna Road",
                        "theme-emerald",
                        List.of("iot", "embedded-systems", "sit", "siliguri", "ece")),
                // 4. NBU - Tea Science
                new SeedCourseDefinition(
                        "c_nbu_tea",
                        subir.getId(),
                        "Tea Plantation Management & Tasting Quality",
                        "M.Sc Tea Science 2nd Sem",
                        "Tea Science and Management",
                        "Comprehensive study of tea bush agronomy, Dooars-Terai pest control, plucking regimes, and international sensory tea evaluation.",
                        "Agro Lab 1, NBU Campus, Raja Rammohunpur",
                        "theme-amber",
                        List.of("tea-science", "nbu", "darjeeling", "dooars", "agriculture")),
                // 5. JGEC IT - Cloud Distributed Systems
                new SeedCourseDefinition(
                        "c_jgec_cloud",
                        arindam.getId(),
                        "Cloud Computing & Distributed Systems",
                        "6th Sem IT",
                        "Information Technology",
                        "Distributed consensus, cloud virtualization, Kubernetes architectures, and serverless compute paradigms.",
                        "IT Seminar Hall, JGEC",
                        "theme-cyan",
                        List.of("cloud-computing", "jgec", "distributed-systems", "docker")),
                // 6. JGEC Civil - Geotechnical
                new SeedCourseDefinition(
                        "c_jgec_civil",
                        arindam.getId(),
                        "Geotechnical & Hill Slope Stabilization in Dooars",
                        "7th Sem Civil",
                        "Civil Engineering",
                        "Slope stability analysis, soil mechanics, and seismic-resistant embankment design for North Bengal hilly terrain and riverine floodplains.",
                        "Structural Lab, JGEC Jalpaiguri",
                        "theme-rose",
                        List.of("civil-engineering", "jgec", "geotechnical", "dooars", "darjeeling")),
                // 7. SGP - Microprocessor Architecture
                new SeedCourseDefinition(
                        "c_sgp_micro",
                        amitesh.getId(),
                        "Microprocessor & Microcontroller Architecture",
                        "Diploma CST 4th Sem",
                        "Computer Science and Engineering",
                        "Architecture, assembly programming, and peripheral interfacing for 8085/8051 and ARM Cortex processors.",
                        "Hardware Lab 1, SGP Dabgram, Siliguri",
                        "theme-blue",
                        List.of("polytechnic", "sgp", "siliguri", "microprocessors", "diploma")),
                // 8. JPI - Building Drawing & Surveying
                new SeedCourseDefinition(
                        "c_jpi_survey",
                        amitesh.getId(),
                        "Building Drawing & Surveying Field Practice",
                        "Diploma Civil 3rd Sem",
                        "Civil Engineering",
                        "Practical theodolite surveying, levelling, contour mapping, and CAD municipal building plans for North Bengal urban development.",
                        "Drawing Hall 2, JPI Danguajhar, Jalpaiguri",
                        "theme-orange",
                        List.of("polytechnic", "jpi", "jalpaiguri", "surveying", "civil")));

        Map<String, Course> courseMap = new HashMap<>();

        for (SeedCourseDefinition def : courseDefs) {
            Course course = Course.builder()
                    .ownerId(def.ownerId)
                    .title(def.title)
                    .section(def.section)
                    .subject(def.subject)
                    .description(def.description)
                    .theme(def.theme)
                    .tags(def.tags)
                    .accessType(CourseAccessType.PUBLIC)
                    .status(CourseStatus.ACTIVE)
                    .enrollmentEnabled(true)
                    .createdAt(now.minus(Duration.ofDays(20)))
                    .updatedAt(now.minus(Duration.ofDays(1)))
                    .build();

            course = courseRepository.save(course);
            courseMap.put(def.key, course);

            // Synchronize with CourseDiscovery
            CourseDiscovery discovery = CourseDiscovery.builder()
                    .courseId(course.getId())
                    .title(course.getTitle())
                    .subject(course.getSubject())
                    .tags(course.getTags())
                    .theme(course.getTheme())
                    .accessType(course.getAccessType())
                    .visibility(CourseVisibility.PUBLIC)
                    .status(CourseStatus.ACTIVE)
                    .enrollmentCount(0)
                    .popularityScore(75.0)
                    .lastActivityAt(now.minus(Duration.ofHours(2)))
                    .createdAt(now.minus(Duration.ofDays(20)))
                    .updatedAt(now.minus(Duration.ofDays(1)))
                    .build();

            courseDiscoveryRepository.save(discovery);
        }

        return courseMap;
    }

    private void seedMemberships(
            Map<String, User> userMap,
            Map<String, Course> courseMap,
            Instant now) {
        log.info("Enrolling student and faculty memberships with rich peer overlaps...");

        // Define which users join which courses and their role
        Map<String, List<MembershipAssignment>> courseEnrollments = Map.of(
                "c_jgec_dsa", List.of(
                        new MembershipAssignment("debasmita.ghosh@jgec.ac.in", MembershipRole.OWNER),
                        new MembershipAssignment("ananya.m@jgec.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("rohan.das@sit.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("tathagata.d@sit.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("riya.s@jgec.ac.in", MembershipRole.MEMBER)),
                "c_sit_ml", List.of(
                        new MembershipAssignment("debasmita.ghosh@jgec.ac.in", MembershipRole.OWNER),
                        new MembershipAssignment("rohan.das@sit.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("tathagata.d@sit.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("ananya.m@jgec.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("priyanka.p@sit.ac.in", MembershipRole.MEMBER)),
                "c_sit_iot", List.of(
                        new MembershipAssignment("sourav.sen@sit.ac.in", MembershipRole.OWNER),
                        new MembershipAssignment("bikram.m@jgec.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("pooja.c@sit.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("tanmoy.b@sgpolytechnic.ac.in", MembershipRole.MEMBER)),
                "c_nbu_tea", List.of(
                        new MembershipAssignment("subir.tea@nbu.ac.in", MembershipRole.OWNER),
                        new MembershipAssignment("sneha.roy@nbu.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("debjit.k@nbu.ac.in", MembershipRole.MEMBER)),
                "c_jgec_cloud", List.of(
                        new MembershipAssignment("arindam.it@jgec.ac.in", MembershipRole.OWNER),
                        new MembershipAssignment("priyanka.p@sit.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("ananya.m@jgec.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("riya.s@jgec.ac.in", MembershipRole.MEMBER)),
                "c_jgec_civil", List.of(
                        new MembershipAssignment("arindam.it@jgec.ac.in", MembershipRole.OWNER),
                        new MembershipAssignment("amitava.b@jgec.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("subhamita.r@jpicampus.ac.in", MembershipRole.MEMBER)),
                "c_sgp_micro", List.of(
                        new MembershipAssignment("amitesh.c@sgpolytechnic.ac.in", MembershipRole.OWNER),
                        new MembershipAssignment("tanmoy.b@sgpolytechnic.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("bikram.m@jgec.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("rohan.das@sit.ac.in", MembershipRole.MEMBER)),
                "c_jpi_survey", List.of(
                        new MembershipAssignment("amitesh.c@sgpolytechnic.ac.in", MembershipRole.OWNER),
                        new MembershipAssignment("subhamita.r@jpicampus.ac.in", MembershipRole.MEMBER),
                        new MembershipAssignment("amitava.b@jgec.ac.in", MembershipRole.MEMBER)));

        for (Map.Entry<String, List<MembershipAssignment>> entry : courseEnrollments.entrySet()) {
            Course course = courseMap.get(entry.getKey());
            if (course == null) continue;

            long memberCount = 0;
            for (MembershipAssignment assign : entry.getValue()) {
                User user = userMap.get(assign.email);
                if (user == null) continue;

                CourseMembership membership = CourseMembership.builder()
                        .courseId(course.getId())
                        .userId(user.getId())
                        .role(assign.role)
                        .status(MembershipStatus.ACTIVE)
                        .joinedAt(now.minus(Duration.ofDays(15)))
                        .build();

                courseMembershipRepository.save(membership);
                memberCount++;
            }

            // Update discovery enrollment count
            final long totalMembers = memberCount;
            courseDiscoveryRepository.findByCourseId(course.getId()).ifPresent(d -> {
                d.setEnrollmentCount(totalMembers);
                d.setPopularityScore(50.0 + (totalMembers * 10));
                courseDiscoveryRepository.save(d);
            });
        }
    }

    private void seedActivities(
            Map<String, User> userMap,
            Map<String, Course> courseMap,
            Instant now) {
        log.info("Seeding academic activities (Coursework, Submissions, Grades, Comments, Notifications)...");

        Course dsaCourse = courseMap.get("c_jgec_dsa");
        User debasmita = userMap.get("debasmita.ghosh@jgec.ac.in");
        User ananya = userMap.get("ananya.m@jgec.ac.in");
        User rohan = userMap.get("rohan.das@sit.ac.in");
        User tathagata = userMap.get("tathagata.d@sit.ac.in");

        if (dsaCourse != null && debasmita != null) {
            // 1. Coursework: Assignment
            Coursework avlAssignment = Coursework.builder()
                    .courseId(dsaCourse.getId())
                    .creatorId(debasmita.getId())
                    .title("Assignment 1: AVL Tree & Graph Traversal for North Bengal Transport Routing")
                    .description("Implement AVL Tree balancing and Dijkstra's algorithm to model optimal transport links across Siliguri, Jalpaiguri, Mainaguri, and Alipurduar.")
                    .type(CourseworkType.ASSIGNMENT)
                    .status(CourseworkStatus.PUBLISHED)
                    .pinned(true)
                    .maximumPoints(100)
                    .publishedAt(now.minus(Duration.ofDays(5)))
                    .dueAt(now.plus(Duration.ofDays(5)))
                    .createdAt(now.minus(Duration.ofDays(5)))
                    .updatedAt(now.minus(Duration.ofDays(5)))
                    .build();

            avlAssignment = courseworkRepository.save(avlAssignment);

            // 2. Coursework: Announcement
            Coursework welcomePost = Coursework.builder()
                    .courseId(dsaCourse.getId())
                    .creatorId(debasmita.getId())
                    .title("Welcome to 3rd Sem DSA Laboratory - JGEC CSE")
                    .description("Welcome everyone! Please review the lab syllabus and set up Java 21 / GCC on your workstations before our first session in Lab 3.")
                    .type(CourseworkType.ANNOUNCEMENT)
                    .status(CourseworkStatus.PUBLISHED)
                    .pinned(false)
                    .publishedAt(now.minus(Duration.ofDays(10)))
                    .createdAt(now.minus(Duration.ofDays(10)))
                    .build();

            courseworkRepository.save(welcomePost);

            // 3. Submissions & Grades
            if (ananya != null) {
                Submission ananyaSub = Submission.builder()
                        .courseworkId(avlAssignment.getId())
                        .courseId(dsaCourse.getId())
                        .studentId(ananya.getId())
                        .status(SubmissionStatus.GRADED)
                        .answerText("Implemented AVL tree rotations with complete JUnit test coverage for Dooars junction vertices. Time complexity O(E + V log V).")
                        .score(BigDecimal.valueOf(96))
                        .graderId(debasmita.getId())
                        .feedback("Outstanding implementation and rigorous asymptotic analysis!")
                        .submittedAt(now.minus(Duration.ofDays(2)))
                        .gradedAt(now.minus(Duration.ofHours(12)))
                        .build();

                submissionRepository.save(ananyaSub);

                StudentGradebookEntry ananyaGrade = StudentGradebookEntry.builder()
                        .courseId(dsaCourse.getId())
                        .studentId(ananya.getId())
                        .courseworkId(avlAssignment.getId())
                        .title(avlAssignment.getTitle())
                        .maximumPoints(100)
                        .score(BigDecimal.valueOf(96))
                        .status(SubmissionStatus.GRADED)
                        .feedback("Outstanding implementation and rigorous asymptotic analysis!")
                        .dueAt(avlAssignment.getDueAt())
                        .gradedAt(now.minus(Duration.ofHours(12)))
                        .generatedAt(now)
                        .build();

                studentGradebookEntryRepository.save(ananyaGrade);
            }

            if (rohan != null) {
                Submission rohanSub = Submission.builder()
                        .courseworkId(avlAssignment.getId())
                        .courseId(dsaCourse.getId())
                        .studentId(rohan.getId())
                        .status(SubmissionStatus.TURNED_IN)
                        .answerText("Submitted repository link: AVL tree rotations and Dijkstra implementation handling edge cases for disconnected mountain passes.")
                        .submittedAt(now.minus(Duration.ofHours(6)))
                        .build();

                submissionRepository.save(rohanSub);

                StudentGradebookEntry rohanGrade = StudentGradebookEntry.builder()
                        .courseId(dsaCourse.getId())
                        .studentId(rohan.getId())
                        .courseworkId(avlAssignment.getId())
                        .title(avlAssignment.getTitle())
                        .maximumPoints(100)
                        .status(SubmissionStatus.TURNED_IN)
                        .dueAt(avlAssignment.getDueAt())
                        .generatedAt(now)
                        .build();

                studentGradebookEntryRepository.save(rohanGrade);
            }

            // 4. Comments on Coursework
            if (tathagata != null) {
                Comment comment1 = Comment.builder()
                        .courseId(dsaCourse.getId())
                        .authorId(tathagata.getId())
                        .targetType(CommentTargetType.COURSEWORK)
                        .targetId(avlAssignment.getId())
                        .visibility(CommentVisibility.PUBLIC)
                        .body("Mam, should we account for monsoon transit road closures as dynamic edge weight infinity?")
                        .createdAt(now.minus(Duration.ofDays(3)))
                        .build();

                commentRepository.save(comment1);

                Comment reply = Comment.builder()
                        .courseId(dsaCourse.getId())
                        .authorId(debasmita.getId())
                        .targetType(CommentTargetType.COURSEWORK)
                        .targetId(avlAssignment.getId())
                        .visibility(CommentVisibility.PUBLIC)
                        .body("Yes Tathagata, representing inaccessible mountain roads with an infinite or high weight edge is a great design choice.")
                        .createdAt(now.minus(Duration.ofDays(2)))
                        .build();

                commentRepository.save(reply);
            }

            // 5. Notifications
            if (ananya != null) {
                Notification notif1 = Notification.builder()
                        .recipientId(ananya.getId())
                        .type(NotificationType.SUBMISSION_GRADED)
                        .title("Assignment Graded")
                        .message("Prof. Debasmita Ghosh graded your submission for Assignment 1: 96/100")
                        .resourceType(NotificationResourceType.COURSEWORK)
                        .resourceId(avlAssignment.getId())
                        .read(false)
                        .createdAt(now.minus(Duration.ofHours(12)))
                        .build();

                notificationRepository.save(notif1);
            }

            if (rohan != null) {
                Notification notif2 = Notification.builder()
                        .recipientId(rohan.getId())
                        .type(NotificationType.COURSEWORK_PUBLISHED)
                        .title("New Coursework Posted")
                        .message("New assignment posted in Data Structures & Algorithms Laboratory")
                        .resourceType(NotificationResourceType.COURSEWORK)
                        .resourceId(avlAssignment.getId())
                        .read(true)
                        .readAt(now.minus(Duration.ofHours(8)))
                        .createdAt(now.minus(Duration.ofDays(5)))
                        .build();

                notificationRepository.save(notif2);
            }
        }

        // Additional Assignment for IoT & Smart Siliguri Course
        Course iotCourse = courseMap.get("c_sit_iot");
        User sourav = userMap.get("sourav.sen@sit.ac.in");
        User bikram = userMap.get("bikram.m@jgec.ac.in");

        if (iotCourse != null && sourav != null) {
            Coursework iotAssignment = Coursework.builder()
                    .courseId(iotCourse.getId())
                    .creatorId(sourav.getId())
                    .title("Assignment 1: Mahananda River Ultrasonic Flood Sensor Node")
                    .description("Design an ESP32 microcontroller circuit diagram and firmware using MQTT to report telemetry during heavy rains.")
                    .type(CourseworkType.ASSIGNMENT)
                    .status(CourseworkStatus.PUBLISHED)
                    .maximumPoints(50)
                    .publishedAt(now.minus(Duration.ofDays(4)))
                    .dueAt(now.plus(Duration.ofDays(6)))
                    .createdAt(now.minus(Duration.ofDays(4)))
                    .build();

            iotAssignment = courseworkRepository.save(iotAssignment);

            if (bikram != null) {
                Submission bikramSub = Submission.builder()
                        .courseworkId(iotAssignment.getId())
                        .courseId(iotCourse.getId())
                        .studentId(bikram.getId())
                        .status(SubmissionStatus.GRADED)
                        .answerText("Attached ESP32 circuit schematic with HC-SR04 ultrasonic sensor and solar trickle charging module.")
                        .score(BigDecimal.valueOf(48))
                        .graderId(sourav.getId())
                        .feedback("Excellent hardware circuit choice and low-power sleep implementation.")
                        .submittedAt(now.minus(Duration.ofDays(1)))
                        .gradedAt(now.minus(Duration.ofHours(4)))
                        .build();

                submissionRepository.save(bikramSub);
            }
        }
    }

    private record SeedUserDefinition(String email, boolean canCreateCourses, boolean isAdmin) {}

    private record SeedProfileDefinition(
            String email,
            String handle,
            String firstName,
            String lastName,
            String displayName,
            String headline,
            String about,
            String city,
            String country,
            String gender,
            boolean canCreateCourses) {}

    private record SeedCourseDefinition(
            String key,
            String ownerId,
            String title,
            String section,
            String subject,
            String description,
            String room,
            String theme,
            List<String> tags) {}

    private record MembershipAssignment(String email, MembershipRole role) {}
}
