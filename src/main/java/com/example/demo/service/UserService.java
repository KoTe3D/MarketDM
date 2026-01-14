package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
//          Данные ниже мы обычно так не прописываем а берём из бд
//        return List.of(
//                new User(1L, "Sergey", "ser@mail.ru", LocalDate.of(1990, 1, 1), 35),
//                new User(2L, "Mary", "mary@mail.ru", LocalDate.of(1991, 2, 2), 34),
//                new User(3L, "Ivan", "ivan@mail.ru", LocalDate.of(1992, 3, 3), 33)
//        );
    }

    public User create(User user) {// Мы хотим не просто сохранить нашего юзера, но и проверить что он ещё не существует в бз
        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());// мы создадим свой собстенный метод в UserRepository так как существующий findBy слишком сложный
        if (optionalUser.isPresent()) {
            throw new IllegalStateException("Юзер с таким никнеймом уже существует");
        }
        user.setAge(Period.between(user.getBirth(), LocalDate.now()).getYears()); //В библеотеке Javatime есть такой статичный класс период
        //вернули разницу между датой рождения и текущей датой и достали из неё год
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Юзер с id " + id + " не существует"));

        userRepository.delete(user);//выше хотим убедиться что объект существует
    }

    public void update(Long id, String email, String name) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            throw new IllegalStateException("Юзер с id " + id + " не существует");
    }
    User user = optionalUser.get();

    if (email != null && !email.equals(user.getEmail())){
        Optional<User> foundByEmail = userRepository.findByEmail(email);// мы создадим свой собстенный метод в UserRepository так как существующий findBy слишком сложный
        if (foundByEmail.isPresent()) {
            throw new IllegalStateException("Юзер с таким никнеймом уже существует");
        }
        user.setEmail(email);
    }

    if (name != null && !name.equals(user.getName())){
        user.setName(name);
    }

        userRepository.save(user);
    }
}
