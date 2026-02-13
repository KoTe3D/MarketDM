/*package com.example.demo.controller;

import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.entity.User;

import java.util.List;
//Вся бизнес-логика не должна храниться в контроллере, по SOLID-принципам каждый класс должен отвечать единичной ответственности, он не должен выполнять
//какую-либо логику, в данном случае билдить юзеров, контроллер на продакшен-коде чаще всего выполняет только одну единственную функцию — это обработка входящих
//запросов, дальше он с ними ничего не делает и делегирует всю логику выполнения слою сервисов, и дальше сервис уже
// с ними работает.
@RestController
@RequestMapping(path = "api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> findAll() {
        return userService.findAll();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return userService.create(user);
    }

    @DeleteMapping(path = "{id}")// здесь будет путь аргумент Long id нужно пометить аннотацией @PathVariable и можно указать name = если имя отличается в нашем случае нет
    public void delete(@PathVariable (name = "id") Long id){// будем передавать объект не в теле метода не в качестве параметров url у нас переменная пути
        userService.delete(id);
    }

}
*/

package com.MarketDM.controller;

import org.springframework.web.bind.annotation.RequestBody;// для обычной аннотации PUT с использованием Json в Postmen
import com.MarketDM.service.UserService;
import org.springframework.web.bind.annotation.*;
import com.MarketDM.entity.User;
import com.MarketDM.DTO.UserUpdateDto;


import java.util.List;

@RestController
@RequestMapping(path = "api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> findAll() {
        return userService.findAll();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        return userService.create(user);
    }

    @DeleteMapping(path = "{id}")
    public void delete(@PathVariable Long id) {
        //try {
            userService.delete(id);
            //return ResponseEntity.ok("Пользователь с ID " + id + " успешно удален");
        //} catch (Exception e) {
            //return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            //        .body("Ошибка: " + e.getMessage());
        //}
    }

    /*public void update(
            @PathVariable Long id,
            @RequestParam(required = false) String email,// данная аннотация предполагает что мы будем передавать данные в URL
            @RequestParam(required = false) String name// то есть если работать через Postmen нужно будет поменять или иметь этот ввиду ведь через Json данные не уйдут хоть статус и будет 200
    ){
        userService.update(id, email, name);

    }
*/
    @PutMapping(path = "{id}")
    public void update(
            @PathVariable Long id,
            @RequestBody UserUpdateDto updateDto  // ← Принимаем DTO
    ){
        userService.update(id, updateDto);
    }

/*
    @PutMapping(path = "{id}")
    public void update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates  // ← Принимаем JSON!
    ){
        userService.update(id, updates);
    }
*/
}