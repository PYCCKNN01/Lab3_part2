import billing.Billing;
import exception.DuplicateSubscriptionException;
import exception.InvalidSubscriptionException;
import exception.SubscriptionNotFoundException;
import model.CorporateSubscription;
import model.FanZonePass;
import model.PromoCode;
import model.Subscription;
import model.UserSubscription;
import net.datafaker.Faker;
import service.NotificationService;
import service.impl.EmailNotification;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        NotificationService emailService = new EmailNotification();
        Billing billing = new Billing(emailService);

        //Генерация 50 пользователей через Datafaker
        generateTestUsers(billing, 50);

        //Демонстрация аналитики через Stream API
        System.out.println("\n=== АНАЛИТИКА STREAM API ===");
        System.out.println("Всего записей в базе: " + billing.getDatabase().size());
        System.out.println("Общая прогнозируемая выручка: " + billing.calculateTotalRevenue() + " руб.");
        System.out.println("Количество VIP_LOGE подписок: " + billing.countByTier("VIP_LOGE"));

        //Поиск VIP-клиентов
        List<Subscription> vipClients = billing.findVipClients(8000.0);
        System.out.println("VIP-клиентов (чек > 8000 руб.): " + vipClients.size());
        vipClients.stream()
                .limit(5) //Выводим первых 5 для наглядности
                .forEach(sub -> System.out.printf("  * %s [%s] - %.2f руб. (опций: %d)%n",
                        sub.getOwnerName(), sub.getTier(), sub.calculateBillingPrice(), sub.getAppliedOptions().size()));


        System.out.println("\n=== ПРОВЕРКА ОБРАБОТКИ ИСКЛЮЧЕНИЙ ===");
        try {
            // Берем первый попавшийся реальный email из сгенерированной базы и проводим оплату
            String existingEmail = billing.getDatabase().keySet().iterator().next();
            billing.processPaymentByEmail(existingEmail);

            billing.processPaymentByEmail("fake_email@yandex.ru");
        } catch (SubscriptionNotFoundException e) {
            System.out.println("[Перехвачено проверяемое исключение]: " + e.getMessage());
        }

        try {
            //Пытаемся создать подписку с отрицательной ценой
            new UserSubscription("Тест ошибки", -500.0, "FAN_ZONE");
        } catch (InvalidSubscriptionException e) {
            System.out.println("[Перехвачено непроверяемое исключение]: " + e.getMessage());
        }

        try {
            //Пытаемся создать подписку с пустым именем
            new UserSubscription("   ", 500.0, "FAN_ZONE");
        } catch (InvalidSubscriptionException e) {
            System.out.println("[Перехвачено непроверяемое исключение]: " + e.getMessage());
        }
    }

    /**
     * Метод-генератор тестовых пользователей с помощью библиотеки Datafaker.
     */
    private static void generateTestUsers(Billing billing, int count) {
        Faker faker = new Faker(new Locale("ru"));       // имена и компании — по-русски
        Faker emailFaker = new Faker(Locale.ENGLISH);    // email — латиницей, пройдёт EMAIL_PATTERN
        String[] tiers = {"DANCE_FLOOR", "FAN_ZONE", "VIP_LOGE"};
        String[] availableOptions = {"Fast Track", "VIP Bar", "Meet & Greet", "Мерч-пакет", "Ранний вход"};

        for (int i = 0; i < count; i++) {
            String email = i + "_" + emailFaker.internet().emailAddress();
            String name = faker.name().fullName();
            String tier = tiers[faker.random().nextInt(tiers.length)];
            double basePrice = faker.number().randomDouble(2, 1000, 5000);

            Subscription sub;
            //Каждый 5-й клиент
            if (i % 5 == 0) {
                sub = CorporateSubscription.builder()
                        .ownerName(faker.company().name())
                        .basePrice(basePrice)
                        .tier(tier)
                        .activeLicensesCount(faker.number().numberBetween(2, 15))
                        .build();
            } else {
                sub = UserSubscription.builder()
                        .ownerName(name)
                        .basePrice(basePrice)
                        .tier(tier)
                        .build();

                //Добавляем от 1 до 3 случайных опций
                int optionsCount = faker.number().numberBetween(1, 4);
                for (int j = 0; j < optionsCount; j++) {
                    String optName = availableOptions[faker.random().nextInt(availableOptions.length)];
                    double optPrice = faker.number().numberBetween(500, 3000);
                    sub.addOption(FanZonePass.builder()
                            .name(optName)
                            .price(optPrice)
                            .build());
                }

                //С вероятностью 50% применяем промокод
                if (faker.bool().bool()) {
                    sub.setAppliedPromo(PromoCode.builder()
                            .code("FAN" + faker.number().digits(2))
                            .discountPercent(faker.number().numberBetween(10, 30))
                            .build());
                }
            }

            try {
                billing.registerSubscription(email, sub);
            } catch (DuplicateSubscriptionException e) {
                System.err.println("[ОШИБКА РЕГИСТРАЦИИ]: " + e.getMessage());
            }
        }
    }
}



//import billing.Billing;
//import exception.DuplicateSubscriptionException;
//import exception.InvalidSubscriptionException;
//import exception.SubscriptionNotFoundException;
//import model.CorporateSubscription;
//import model.FanZonePass;
//import model.PromoCode;
//import model.Subscription;
//import model.UserSubscription;
//import net.datafaker.Faker;
//import service.NotificationService;
//import service.impl.EmailNotification;
//
//import java.util.Locale;
//
//public class Main {
//    public static void main(String[] args) {
//        NotificationService emailService = new EmailNotification();
//        Billing billing = new Billing(emailService);
//
//        //Генерация 50 пользователей через Datafaker
//        generateTestUsers(billing, 50);
//
//
//        System.out.println("\nВсего записей в базе: " + billing.getDatabase().size());
//        System.out.println("Примеры сгенерированных данных:");
//        billing.getDatabase().entrySet().stream()
//                .limit(5) //Первые 5 для наглядности
//                .forEach(entry -> System.out.printf("  * %s -> %s [%s], чек: %.2f руб.%n",
//                        entry.getKey(), entry.getValue().getOwnerName(),
//                        entry.getValue().getTier(), entry.getValue().calculateBillingPrice()));
//
//
//        System.out.println("\n=== ПРОВЕРКА ОБРАБОТКИ ИСКЛЮЧЕНИЙ ===");
//        try {
//
//            String existingEmail = billing.getDatabase().keySet().iterator().next();
//            billing.processPaymentByEmail(existingEmail);
//
//
//            billing.processPaymentByEmail("fake_user@yandex.ru");
//        } catch (SubscriptionNotFoundException e) {
//            System.out.println("[Перехвачено проверяемое исключение]: " + e.getMessage());
//        }
//
//        try {
//            //Пытаемся создать подписку с отрицательной ценоq
//            new UserSubscription("Тест oшибки", -500.0, "FAN_ZONE");
//        } catch (InvalidSubscriptionException e) {
//            System.out.println("[Перехвачено непроверяемое исключение]: " + e.getMessage());
//        }
//
//        try {
//            //Пытаемся создать подписку с пустым именем
//            new UserSubscription("   ", 500.0, "FAN_ZONE");
//        } catch (InvalidSubscriptionException e) {
//            System.out.println("[Перехвачено непроверяемое исключение]: " + e.getMessage());
//        }
//
//        System.out.println();
//        Billing.printGlobalStats();
//    }
//
//    /**
//     * Метод-генератор тестовых пользователей с помощью библиотеки Datafaker.
//     */
//    private static void generateTestUsers(Billing billing, int count) {
//        Faker faker = new Faker(new Locale("ru"));       // имена и компании — по-русски
//        Faker emailFaker = new Faker(Locale.ENGLISH);    // email — латиницей, пройдёт EMAIL_PATTERN
//        String[] tiers = {"DANCE_FLOOR", "FAN_ZONE", "VIP_LOGE"};
//        String[] availableOptions = {"Fast Track", "VIP Bar", "Афтограф", "Мерч-пакет", "Что-то еще"};
//
//        for (int i = 0; i < count; i++) {
//            // Индекс i гарантирует уникальность, emailFaker — латиницу в адресе
//            String email = i + "_" + emailFaker.internet().emailAddress();
//            String name = faker.name().fullName();
//            String tier = tiers[faker.random().nextInt(tiers.length)];
//            double basePrice = faker.number().randomDouble(2, 1000, 5000);
//
//            Subscription sub;
//            //Каждый 5-й клиент корпоративный
//            if (i % 5 == 0) {
//                sub = CorporateSubscription.builder()
//                        .ownerName(faker.company().name())
//                        .basePrice(basePrice)
//                        .tier(tier)
//                        .activeLicensesCount(faker.number().numberBetween(2, 15))
//                        .build();
//            } else {
//                sub = UserSubscription.builder()
//                        .ownerName(name)
//                        .basePrice(basePrice)
//                        .tier(tier)
//                        .build();
//
//                //Добавляем от 1 до 3 случайных пропусков
//                int optionsCount = faker.number().numberBetween(1, 4);
//                for (int j = 0; j < optionsCount; j++) {
//                    String optName = availableOptions[faker.random().nextInt(availableOptions.length)];
//                    double optPrice = faker.number().numberBetween(500, 3000);
//                    sub.addOption(FanZonePass.builder()
//                            .name(optName)
//                            .price(optPrice)
//                            .build());
//                }
//
//                //С вероятностью 50% применяем промокод
//                if (faker.bool().bool()) {
//                    sub.setAppliedPromo(PromoCode.builder()
//                            .code("FAN" + faker.number().digits(2))
//                            .discountPercent(faker.number().numberBetween(10, 30))
//                            .build());
//                }
//            }
//
//            try {
//                billing.registerSubscription(email, sub);
//            } catch (DuplicateSubscriptionException e) {
//                System.err.println("[ОШИБКА РЕГИСТРАЦИИ]: " + e.getMessage());
//            }
//        }
//    }
//}



/*


import billing.Billing;
import exception.DuplicateSubscriptionException;
import exception.InvalidSubscriptionException;
import exception.SubscriptionNotFoundException;
import model.CorporateSubscription;
import model.FanZonePass;
import model.Subscription;
import model.UserSubscription;
import model.PromoCode;
import service.impl.EmailNotification;

public class Main {
    public static void main(String[] args) {
        Billing billing = new Billing(new EmailNotification());

        // --- Подготовка успешных данных ---
        //Различные опции
        //Было: new FanZonePass("Fast Track", 500.0) - теперь через @Builder
        FanZonePass fastTrack = FanZonePass.builder().name("Fast Track").price(500.0).build();
        FanZonePass vipBar = FanZonePass.builder().name("VIP Bar").price(1500.0).build();

        // Успешная подписка (билет) 1 (Физлицо, тариф FAN_ZONE)
        Subscription sub1 = new UserSubscription("Иван Иванов", 2000.0, "FAN_ZONE");
        sub1.addOption(fastTrack);
        sub1.addOption(vipBar);

        //Демонстрация @Builder: пошаговое создание промокода и подключение скидки
        PromoCode fan20 = PromoCode.builder()
                .code("FAN20")
                .discountPercent(20)
                .build();
        sub1.setAppliedPromo(fan20);

// Демонстрация @ToString от Lombok
        System.out.println(sub1);

        // Успешная подписка (билет) 2 (Корпоратив на 5 лицензий, тариф DANCE_FLOOR)
        Subscription sub2 = new CorporateSubscription("ООО 'Вектор'", 3000.0, "DANCE_FLOOR", 5);
        sub2.addOption(fastTrack);

        Subscription sub3 = new UserSubscription("Дима Долинин", 2000.0, "FAN_ZONE");
        Subscription sub4 = new UserSubscription("Вова Долинин", 2000.0, "FAN_ZONE");


        System.out.println("");

        // Регистрация в базе
        try {
            billing.registerSubscription("ivanov@mail.ru", sub1);
            billing.registerSubscription("corporate@vector.com", sub2);
            billing.registerSubscription("dolinin@mail.ru", sub3);
            billing.registerSubscription("dolinin@mail.ru", sub4);
        } catch (DuplicateSubscriptionException e) {
            System.out.println("[ОШИБКА РЕГИСТРАЦИИ]: " + e.getMessage());
            System.err.println("[ОШИБКА РЕГИСТРАЦИИ]: " + e.getMessage());
        }

        // Сценарий 1: Успех
        System.out.println("\n--- Сценарий 1: Успешная оплата ---");
        try {
            billing.processPaymentByEmail("ivanov@mail.ru");
            billing.processPaymentByEmail("corporate@vector.com");
        } catch (SubscriptionNotFoundException e) {
            System.out.println("Эта строчка не должна выполниться: " + e.getMessage());
        }

        // Сценарий 2: Проверяемое исключение (SubscriptionNotFoundException)
        System.out.println("\n--- Сценарий 2: Попытка оплаты несуществующего Email ---");
        try {
            billing.processPaymentByEmail("fake@mail.com");
        } catch (SubscriptionNotFoundException e) {
            System.out.println("[ПЕРЕХВАЧЕНО ИСКЛЮЧЕНИЕ]: " + e.getMessage());
            System.err.println("[ПЕРЕХВАЧЕНО ИСКЛЮЧЕНИЕ]: " + e.getMessage());
        }

        // Сценарий 3: Непроверяемое исключение (InvalidSubscriptionException)
        System.out.println("\n--- Сценарий 3.1: Создание некорректной подписки (отрицательная цена) ---");
        try {
            Subscription badSub = new UserSubscription("Петр Петров", -100.0, "FAN_ZONE");
        } catch (InvalidSubscriptionException e) {
            System.out.println("[ПЕРЕХВАЧЕНО ИСКЛЮЧЕНИЕ]: " + e.getMessage());
            System.err.println("[ПЕРЕХВАЧЕНО ИСКЛЮЧЕНИЕ]: " + e.getMessage());
        }

        System.out.println("\n--- Сценарий 3.2: Создание некорректной подписки (пустое имя) ---");
        try {
            Subscription badSub2 = new UserSubscription("   ", 500.0, "FAN_ZONE");
        } catch (InvalidSubscriptionException e) {
            System.out.println("[ПЕРЕХВАЧЕНО ИСКЛЮЧЕНИЕ]: " + e.getMessage());
            System.err.println("[ПЕРЕХВАЧЕНО ИСКЛЮЧЕНИЕ]: " + e.getMessage());
        }


        System.out.println();
        Billing.printGlobalStats();
        System.out.println("");
    }
}

 */