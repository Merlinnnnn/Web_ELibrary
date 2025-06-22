package com.spkt.libraSys.aData;

import com.spkt.libraSys.service.document.DocumentType.DocumentTypeEntity;
import com.spkt.libraSys.service.document.DocumentType.DocumentTypeRepository;
import com.spkt.libraSys.service.document.course.CourseEntity;
import com.spkt.libraSys.service.document.course.CourseRepository;
import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.role.RoleRepository;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import com.spkt.libraSys.service.drm.key.KeyPairManagementService;
import com.spkt.libraSys.service.user.UserStatus;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DataInit implements CommandLineRunner {
    CourseRepository courseRepository;
     DocumentTypeRepository DocumentTypeEntityRepository;
     RoleRepository roleRepository;
     UserRepository userRepository;
     PasswordEncoder passwordEncoder;
     KeyPairManagementService keyPairManagementService;

    @Override
    @Transactional // Thêm annotation này
    public void run(String... args) throws Exception {

        if(roleRepository.count() <1){
            RoleEntity roleADMIN = RoleEntity.builder()
                    .roleName("ADMIN")
                    .build();
            RoleEntity roleUSER = RoleEntity.builder()
                    .roleName("USER")
                    .build();
            RoleEntity roleManager = RoleEntity.builder()
                    .roleName("MANAGER")
                    .build();
            roleRepository.saveAll(List.of(roleADMIN,roleUSER,roleManager) );
            Set<RoleEntity> roleList = new HashSet<>();

            roleList.add(roleManager);
            roleList.add(roleUSER);
            roleList.add(roleADMIN);
            UserEntity manager = UserEntity.builder()
                    .username("manager@gmail.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("first")
                    .lastName("last")
                    .roleEntities(new HashSet<>(List.of(roleManager)))
                    .isActive(UserStatus.ACTIVE)
                    .build();

            UserEntity user = UserEntity.builder()
                    .username("21110622@gmail.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("first")
                    .lastName("last")
                    .roleEntities(new HashSet<>(List.of(roleUSER)))
                    .isActive(UserStatus.ACTIVE)
                    .build();


            UserEntity admin = UserEntity.builder()
                    .username("admin@gmail.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("first")
                    .lastName("last")
                    .roleEntities(roleList)
                    .isActive(UserStatus.ACTIVE)
                    .build();

            userRepository.saveAll(List.of(user,manager,admin));
        }



        // 2. Tạo và lưu Courses nếu chưa có
//        if (courseRepository.count() < 1) {
//            List<CourseEntity> courses = Arrays.asList(
//                    CourseEntity.builder()
//                            .courseCode("GEFC220105")
//                            .courseName("Kinh tế học đại cương")
//                            .description("Mô tả môn Kinh tế học đại cương")
//                            .build(),
//                    CourseEntity.builder()
//                            .courseCode("IQMA220205")
//                            .courseName("Nhập môn quản trị chất lượng")
//                            .description("Mô tả môn Nhập môn quản trị chất lượng")
//                            .build(),
//                    CourseEntity.builder()
//                            .courseCode("INMA220305")
//                            .courseName("Nhập môn Quản trị học")
//                            .description("Mô tả môn Nhập môn Quản trị học")
//                            .build(),
//                    CourseEntity.builder()
//                            .courseCode("INLO220405")
//                            .courseName("Nhập môn Logic học")
//                            .description("Mô tả môn Nhập môn Logic học")
//                            .build(),
//                    CourseEntity.builder()
//                            .courseCode("TOEN430979")
//                            .courseName("Công cụ và môi trường phát triển PM")
//                            .description("Mô tả môn Công cụ và môi trường phát triển PM")
//                            .build()
//            );
//
//            courseRepository.saveAll(courses);
//        }

        // 3. Tạo và lưu  DocumentTypeEntity nếu chưa có
//        if ( DocumentTypeEntityRepository.count() < 4) {
//             DocumentTypeEntity type1 =  DocumentTypeEntity.builder()
//                    .typeName("Fiction")
//                    .description("Fictional Books")
//                    .build();
//
//             DocumentTypeEntity type2 =  DocumentTypeEntity.builder()
//                    .typeName("Science")
//                    .description("Scientific Journals and Books")
//                    .build();
//
//             DocumentTypeEntity type3 =  DocumentTypeEntity.builder()
//                    .typeName("History")
//                    .description("Historical Documents")
//                    .build();
//
//             DocumentTypeEntity type4 =  DocumentTypeEntity.builder()
//                    .typeName("Technology")
//                    .description("Tech-related Manuals and Guides")
//                    .build();
//
//             DocumentTypeEntityRepository.saveAll(Arrays.asList(type1, type2, type3, type4));
//
//        }

    }
}