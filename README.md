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
### Вариант 1: извлекая из входного потока необходимые данные, получаем полную картину, и на ее основе выстраиваем нужные нам конструкции.
Для этого входной List\<UserMeals\> прогоняем сначала через первый stream, где получаем суммарные данные по каждому дню, заносим их в HashMap\<Date, Calories\>, затем вторым проходом преобразуем List\<UserMeals\> в List\<UserMealWithExceed\>, добавляя результат сравнения калорий в каждом приеме пищи с суммарным суточным значением из HashMap; результат фильтруем, получая на выходе список в заданных временных рамках.<br>
Для простоты сравнения в класс UserMeals не помешает добавить вспомогательный метод, возвращающий в удобном формате дату без времени:
```
  public LocalDate getDate() {
        return LocalDate.from(dateTime);
    }
```
Вопрос, который поначалу кажется неразрешимым - как соединить два потока в одну конструкцию - после прочтения документации легко решается через использование метода collectingAndThen класса java.util.stream.Collectors:
```
public static <T,A,R,RR> Collector<T,A,RR> collectingAndThen(Collector<T,A,R> downstream, Function<R,RR> finisher)
```
Метод позволяет добавить к первому сборщику дополнительную финишную обработку, куда мы поместим второй проход по списку. В конечном итоге получаем что то вроде:
```
  //первый проход по списку mealList
  return mealList
                .stream()
                .collect(
                        //сначала запускается сборщик(коллектор), а за ним последует финишная обработка
                        Collectors.collectingAndThen(
                                //собираем данные в набор пар ключ - "дата", значение - "сумма калорий"
                                Collectors.groupingBy(UserMeal::getDate,
                                        Collectors.summingInt(UserMeal::getCalories)),
                                //передаем HashMap дальше
                                map -> {
                                    //начинается второй проход, ранее заявленный как финишная обработка после первого сборщика
                                    return mealList
                                            .stream()
                                            //фильтруем список в заданных временных рамках
                                            .filter(x ->
                                                    TimeUtil.isBetween(LocalTime.from(x.getDateTime()), startTime, endTime))
                                            //преобразуем в UserMealWithExceed
                                            .map(x -> {
                                                return new UserMealWithExceed(
                                                        x.getDateTime(),
                                                        x.getDescription(),
                                                        x.getCalories(),
                                                        new Boolean(map.get(x.getDate()) > caloriesPerDay));
                                            })
                                            //собираем в итоговый список
                                            .collect(Collectors.toList());
                                }
                        ));
```
Мы сначала описываем, что хотим получить, а потом запускаем процесс вызовом коллекторов(сборщиков). Так мы реализуем функциональный подход, появившийся в Java 8 с добавлением stream-потоков "ленивых" вычислений.

### Вариант 2: последовательно перебирая входной поток, приступаем к созданию фрагментов итогового списка, как только накопится минимально необходимый объем данных. 
Процесс состоит из множества коротких последовательно повторяющихся операций - это позволяет отказаться от промежуточного хранилища в виде объемной HashMap, а для разграничения серий использовать естественные границы отсортированных данных. Каждый раз со сменой даты мы начинаем новую итерацию, читаем в буфер входящие объекты, в конце - суммируем калории за день, затем сравниваем с ограничением и выдаем "на гора" соответствующую порцию объектов итогового списка. Некое "скользящее окно", открывающееся и закрывающееся со сменой дня.<br>
Замена работы с HashMap сортировкой данных правомерно вызывает вопросы относительно эффективности такого решения, однако используемый, начиная с Java 7, алгоритм сортировки Timsort очень хорош(O(N) - O(NlogN)) на уже частично сортированных данных, а именно такими мы и оперируем. Вместе с ограниченным объемом входящих записей это дает нам все основания полагать, что применение сортировки не является критичным, особенно учитывая простоту реализации решения задачи всего одним традиционным циклом.<br>
Как это часто бывает, где хорошо работают процедурные методы, функциональные оказываются совсем не так удобны и требуют хитрых танцев с бубном: нужно либо выносить объявление и инициализацию переменных из непрерывного потока, где мы не можем их объявить в силу синтаксических ограничений Java 8 stream, или писать специальную реализацию коллектора, где можно поместить все необходимые поля: текущую дату, сумму калорий за день и промежуточный буфер для хранения входящих объектов до смены даты.<br>
В общих чертах коллектор будет иметь примерно такую конструкцию:
```
public class SlidingCollector<T> implements Collector<T, List<List<T>>, List<List<T>>> {
    private List<UserMeal> bufferedList = new ArrayList<>();
    private LocalDate bufferedDate = LocalDate.of(1900, 1, 1);
    private int totalCaloriesPerDay;

    @Override
    public Supplier<List<List<T>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<List<T>>, T> accumulator() {
        //вытаскиваем данные из стрима в промежуточный буфер
    }
    
    @Override
    public Function<List<List<T>>, List<List<T>>> finisher() {
        //проверяем смену даты и считаем калории за день
    }
    
    @Override
    public BinaryOperator<List<List<T>>> combiner() {
        //где то тут фильтруем по startTime, endTime, сравниваем totalCaloriesPerDay с внешним caloriesPerDay
        return new UserMealWithExceed(...);
    }

    @Override
    public Set<Characteristics> characteristics() {
        //ничего из этого: CONCURRENT, IDENTITY_FINISH, UNORDERED 
        return EnumSet.noneOf(Characteristics.class);
    }
    
}
```
В общем, коллектор получается громоздкий и не универсальный - вопрос фильтрации по startTime, endTime требует привязки не только к потоку, но и временнЫм ограничителям так же, как к лимиту суточного потребления калорий caloriesPerDay. Одним циклом куда проще.

Что можно сказать в заключение? Читайте документацию - там есть все ответы на "неразрешимые" вопросы (мне это помогло:) и пишите код тем способом, который наболее эффективен, благо, у современной Джавы для этого есть все и даже чуть больше (включая старые и самые простые решения).
