package ru.javawebinar.topjava.util;

import ru.javawebinar.topjava.model.UserMeal;
import ru.javawebinar.topjava.model.UserMealWithExceed;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

/**
 * GKislin
 * 31.05.2015.
 * Реализовать метод UserMealsUtil.getFilteredWithExceeded:
 -  должны возвращаться только записи между startTime и endTime
 -  поле UserMealWithExceed.exceed должно показывать,
 превышает ли сумма калорий за весь день параметра метода caloriesPerDay

 Т.е UserMealWithExceed - это запись одной еды, но поле exceeded будет одинаково для всех записей за этот день.

 - Проверте результат выполнения ДЗ (можно проверить логику в http://topjava.herokuapp.com , список еды)
 - Оцените Time complexity вашего алгоритма, если он O(N*N)- попробуйте сделать O(N).
 */
public class UserMealsUtil {
    public static void main(String[] args) {
        List<UserMeal> mealList = Arrays.asList(
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 30,10,0),
                        "Завтрак",
                        500),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 30,13,0),
                        "Обед",
                        1000),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 30,20,0),
                        "Ужин",
                        500),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 31,10,0),
                        "Завтрак",
                        1000),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 31,13,0),
                        "Обед",
                        500),
                new UserMeal(LocalDateTime.of(2015, Month.MAY, 31,20,0),
                        "Ужин",
                        510));

        List<UserMealWithExceed> mealExceedList = getFilteredWithExceeded(
                mealList,
                LocalTime.of(7, 0),
                LocalTime.of(12,0),
                2000);
        
    }

    public static List<UserMealWithExceed>  getFilteredWithExceeded(List<UserMeal> mealList,
                                                                    LocalTime startTime,
                                                                    LocalTime endTime,
                                                                    int caloriesPerDay) {

        //clear and straightforward: 2 streams, complexity O(N)
        Map<LocalDate, Integer> map = mealList
                .stream()
                .collect(
                        Collectors.groupingBy(
                                UserMeal::getDate,
                                Collectors.reducing(
                                        0,
                                        UserMeal::getCalories,
                                        Integer::sum)));
        List result = mealList
                .stream()
                .filter(x -> TimeUtil.isBetween(LocalTime.from(x.getDateTime()), startTime, endTime))
                .map(x -> new UserMealWithExceed(
                        x.getDateTime(),
                        x.getDescription(),
                        x.getCalories(),
                        map.get(x.getDate()) > caloriesPerDay))
                .collect(Collectors.toList());

        return result;
    }

    public static List<UserMealWithExceed>  getFilteredWithExceededInline(List<UserMeal> mealList,
                                                                    LocalTime startTime,
                                                                    LocalTime endTime,
                                                                    int caloriesPerDay) {

        // the same as above but inline as they asked to do
        return mealList
                .stream()
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(UserMeal::getDate,
                                        Collectors.summingInt(UserMeal::getCalories)),
                                dailyCaloriesMap -> {
                                    return mealList
                                            .stream()
                                            .filter(x ->
                                                    TimeUtil.isBetween(LocalTime.from(x.getDateTime()), startTime, endTime))
                                            .map(x -> {
                                                return new UserMealWithExceed(
                                                        x.getDateTime(),
                                                        x.getDescription(),
                                                        x.getCalories(),
                                                        new Boolean(dailyCaloriesMap.get(x.getDate()) > caloriesPerDay));
                                            })
                                            .collect(Collectors.toList());
                                }
                        ));
    }
}
