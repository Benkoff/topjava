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
Так мы реализуем функциональный подход, появившийся в Java 8 с добавлением stream-потоков "ленивых" вычислений: сначала описываем, что хотим получить, а потом запускаем процесс вызовом терминальных операций - коллекторов(сборщиков). 

### Вариант 2: последовательно перебирая входной поток, приступаем к созданию фрагментов итогового списка, как только накопится минимально необходимый объем данных. 
Процесс состоит из множества коротких последовательно повторяющихся операций - это позволяет отказаться от промежуточного хранилища в виде объемной HashMap, а для разграничения серий использовать естественные границы отсортированных данных. Каждый раз со сменой даты мы начинаем новую итерацию, читаем в буфер входящие объекты, в конце - суммируем калории за день, затем сравниваем с ограничением и выдаем "на гора" соответствующую порцию объектов итогового списка. Некое "скользящее окно", открывающееся и закрывающееся со сменой дня.<br>
Замена работы с HashMap сортировкой данных правомерно вызывает вопросы относительно эффективности такого решения, однако используемый, начиная с Java 7, алгоритм сортировки Timsort очень хорош(O(N) - O(NlogN)) на уже частично сортированных данных, а именно такими мы и оперируем. Вместе с ограниченным объемом входящих записей это дает нам все основания полагать, что применение сортировки не является критичным, особенно учитывая простоту реализации решения задачи всего одним традиционным циклом.<br>
Нужно еще добавить, что вариант последовательного перебора, помимо прочего, решает нас возможности многопоточной реализации (или по меньшей мере существенно ее осложняет).<br>
Как это бывает, где хорошо работают процедурные методы, функциональные порой оказываются совсем не так удобны и требуют хитрых танцев с бубном. Если не передать на входе вместе со списком ссылки на переменные для буферизации объектов и подсчета калорий за день (а это вряд ли упростит написание и, что особенно важно, чтение кода), для них нужно создавать отдельные поля в классе или лезть "под капот" и писать специальную реализацию коллектора, где можно объявить и инициализировать всё необходимое. В общих чертах он будет иметь примерно такую конструкцию:
```
public class SlidingCollector<T> implements Collector<T, List<List<T>>, List<List<T>>> {
    private List<UserMeal> bufferedList = new ArrayList<>();    //буфер для записей
    private LocalDate bufferedDate = LocalDate.of(1900, 1, 1);  //начальная дата для сравнения
    private int totalCaloriesPerDay;                            //калории за день

    @Override
    public Supplier<List<List<T>>> supplier() {
        //контейнер для сбора готовых UserMealWithExceed
        return ArrayList::new;        
    }

    @Override
    public BiConsumer<List<List<T>>, T> accumulator() {
        //вытаскиваем данные из стрима в промежуточный буфер, считаем totalCaloriesPerDay
    }
    
    @Override
    public Function<List<List<T>>, List<List<T>>> finisher() {
        //проверяем смену даты и реализуем логику итераций
        //где то тут же фильтруем по startTime, endTime,
        //сравниваем totalCaloriesPerDay с внешним caloriesPerDay и пишем в UserMealWithExceed
        return new UserMealWithExceed(...);
    }
    
    @Override
    public BinaryOperator<List<List<T>>> combiner() {
        //это нам ни к чему - параллельно наш сортированный список вряд ли будем обрабатывать
        //хотя, сюда можно вынести часть бизнес-логики
    }

    @Override
    public Set<Characteristics> characteristics() {
        //ничего из этого: CONCURRENT, IDENTITY_FINISH, UNORDERED 
        return EnumSet.noneOf(Characteristics.class);
    }
    
}
```
Очевидно коллектор получается громоздкий и не универсальный - вопрос фильтрации по startTime, endTime требует привязки не только к потоку, но и временнЫм ограничителям так же, как к лимиту суточного потребления калорий caloriesPerDay. Можно переписать не отдельным классом, а прямо в стриме: 
```
... сollect(
        Collector.of(
                    ArrayList::new,   //supplier() - метод инициализации аккумулятора,
                    (...) -> {...},   //accumulator() - метод обработки каждого элемента,
                    (...) -> {...},   //combiner() - метод соединения двух аккумуляторов,
                    (...) -> {...}    //finisher() - метод последней обработки аккумулятора 
        ));
```
Но одним циклом все равно проще.
