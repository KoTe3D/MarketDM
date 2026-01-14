package com.example.demo.repository;


import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

//import java.security.SecureRandom;
//import java.time.LocalDate;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {//Название репозитория и тип Id шника
//ниже создание методов разным путём на нативном sql и на postgres
    @Query(value = "select * from users where email = :email", nativeQuery = true)//Это вариант с натив sql nativeQuery = true даёт понять что будет выполняться нативный запрос слева от него
    Optional<User> findByEmail(String email);// Если аргумент имел бы другое значение нужно было-бы написать @Paran(value = "email")

    //@Query(value = "select u from User u where u.email = :email")// тоже что и выше только это вариант с GPsql написанный на специальном языке запросов JPA
    //User findByEmail(String email);

    //User findByEmailAndAgeAfterAndBirth(String email, Integer age, LocalDate birth);//используем спецификацию JPA мы можем писать запрос в виде самого метода используя ключевые слова на сайте spring boota можно посмотреть спецификацию слов
}
