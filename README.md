Java Enterprise Online Project 
===============================

## ![hw](https://cloud.githubusercontent.com/assets/13649199/13672719/09593080-e6e7-11e5-81d1-5cb629c438ca.png) Домашнее задание HW0
```
Реализовать метод UserMealsUtil.getFilteredWithExceeded:
-  должны возвращаться только записи между startTime и endTime 
-  поле UserMealWithExceed.exceed должно показывать, 
                                     превышает ли сумма калорий за весь день параметра метода caloriesPerDay  
        
Т.е UserMealWithExceed - это запись одной еды, но поле exceeded будет одинаково для всех записей за этот день.
    
- Проверте результат выполнения ДЗ (можно проверить логику в http://topjava.herokuapp.com , список еды)
- Оцените Time complexity вашего алгоритма, если он O(N*N)- попробуйте сделать O(N).
```
Очевидно, существует множество различных вариантов решения поставленной задачи в рамках требуемой сложности O(N) (без вложенных циклов и рекурсии), но если не все, то большая их часть, сводятся к двум принципиальным схемам.
### 1. Извлекая из входного потока все необходимые данные, получаем полную картину и на ее основе выстраиваем нужные нам конструкции.
  То есть, входной List<UserMeals> прогоняем сначала через первый stream, где получаем суммарные данные по каждому дню, заносим их в HashMap<Date, Calories>, затем вторым проходом преобразуем List<UserMeals> в List<UserMealWithExceed> через сравнение калорий в каждом приеме пищи с суммарным суточным из HashMap; результат фильтруем, получая на выходе списоок в заданных временных рамках.
  Для простоты сравнения дат в класс UserMeals не помешает добавить вспомогательный метод, возвращающий день в удобном формате:
```
  public LocalDate getDate() {
        return LocalDate.from(dateTime);
    }
```
  Сложность соединения двух потоков в одну конструкцию преодолевается использованием метода стандартного коллектора 
```
  public static <T,A,R,RR> Collector<T,A,RR> collectingAndThen(Collector<T,A,R> downstream, Function<R,RR> finisher)
```
Он позволяет добавить к первому коллектору дополнительную финишную обработку, которой и станет вторая обработка входного списка. В конечном итоге получим что то вроде:
```
  return mealList
                .stream()
                .collect(
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(UserMeal::getDate,
                                        Collectors.summingInt(UserMeal::getCalories)),
                                map -> {
                                    return mealList
                                            .stream()
                                            .filter(x ->
                                                    TimeUtil.isBetween(LocalTime.from(x.getDateTime()), startTime, endTime))
                                            .map(x -> {
                                                return new UserMealWithExceed(
                                                        x.getDateTime(),
                                                        x.getDescription(),
                                                        x.getCalories(),
                                                        new Boolean(map.get(x.getDate()) > caloriesPerDay));
                                            })
                                            .collect(Collectors.toList());
                                }
                        ));
```
  Как раз тот случай, когда сначала описываем, что мы хотим получить, а потом запускаем процесс. Именно то, что прекрасно реализуется при помощи "ленивых" вычислений Java 8 stream.

### 2. Последовательно перебирая входной поток, приступаем к созданию фрагментов итогового списка, как только накопится минимально необходимый объем информации. 
  Так как мы отказываемся от промежуточного хранилища данных в пользу дробных операций, понадобится сортировка по дате. Каждый раз с ее сменой мы начинаем новую итерацию и читаем в буфер входящие объекты, а в конце - суммируем калории за день, сравниваем с ограничением и выдаем "на гора" соответствующую порцию объектов итогового списка. Такое себе "скользящее окно", открывающееся и закрывающееся со сменой дня. Реализация традиционными методами элементарна и приводить ее тут нет никакого смысла. 
  Как это часто бывает, где хорошо работают процедурные методы, функциональные оказываются не так удобны и требуют некоторых танцев с бубном. Или нужно выносить объявление и инициализацию переменных из непрерывного потока, где мы не можем их объявить в силу синтаксических ограничений Java 8 stream, или необходимо писать специальную реализацию коллектора, где также можно объявить и инициализировать текущую дату, сумму калорий за день и промежуточный буфер для хранения входных данных до смены даты.

TBC
