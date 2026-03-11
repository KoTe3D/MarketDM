package com.MarketDM.controller;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        int statusCode = 500;
        String errorTitle = "Ошибка сервера";
        String errorMessage = "Что-то пошло не так. Пожалуйста, попробуйте позже.";

        if (status != null) {
            statusCode = Integer.parseInt(status.toString());

            switch (statusCode) {
                case 400:
                    errorTitle = "Плохой запрос";
                    errorMessage = "Ваш запрос содержит ошибки. Проверьте данные.";
                    break;
                case 401:
                    errorTitle = "Требуется авторизация";
                    errorMessage = "Пожалуйста, войдите в систему.";
                    break;
                case 403:
                    errorTitle = "Доступ запрещён";
                    errorMessage = "У вас нет прав для просмотра этой страницы.";
                    break;
                case 404:
                    errorTitle = "Страница не найдена";
                    errorMessage = "Извините, мы не можем найти то, что вы ищете.";
                    break;
                case 405:
                    errorTitle = "Метод не разрешён";
                    errorMessage = "Этот метод запроса не поддерживается.";
                    break;
                case 500:
                    errorTitle = "Ошибка сервера";
                    errorMessage = "На сервере произошла ошибка. Мы уже работаем над этим.";
                    break;
                case 502:
                case 503:
                case 504:
                    errorTitle = "Сервис временно недоступен";
                    errorMessage = "Сервер перегружен или ведутся технические работы. Попробуйте позже.";
                    break;
                default:
                    errorTitle = "Ошибка " + statusCode;
                    errorMessage = "Произошла неизвестная ошибка.";
            }
        }

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorTitle", errorTitle);
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("path", path);
        model.addAttribute("timestamp", new Date());

        return "error-page"; // → templates/error-page.html
    }
}