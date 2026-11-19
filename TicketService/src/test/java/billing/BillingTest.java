package billing;

import exception.DuplicateSubscriptionException;
import exception.InvalidSubscriptionException;
import exception.SubscriptionNotFoundException;
import model.CorporateSubscription;
import model.FanZonePass;
import model.PromoCode;
import model.Subscription;
import model.UserSubscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.impl.EmailNotification;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class BillingTest {

    private Billing billing;

    @BeforeEach
    void setUp() {
        // Этот метод выполняется автоматически ПЕРЕД каждым тестом, создавая чистую базу
        billing = new Billing(new EmailNotification());
    }

    // Вспомогательный метод: три подписки с заранее известными ценами
    // 1000.0 (DANCE_FLOOR) + 3200.0 (FAN_ZONE) + 10500.0 (VIP_LOGE) = 14700.0
    private void fillDatabaseWithKnownSubscriptions() throws DuplicateSubscriptionException {
        billing.registerSubscription("cheap@concert.ru", new UserSubscription("Анна Дешёвая", 1000.0, "DANCE_FLOOR"));
        billing.registerSubscription("middle@concert.ru", new UserSubscription("Борис Средний", 2000.0, "FAN_ZONE"));
        billing.registerSubscription("vip@concert.ru", new UserSubscription("Виктор ВИП", 3000.0, "VIP_LOGE"));
    }

    @Test
    @DisplayName("Расчёт цены: тариф + несколько опций + скидка по промокоду")
    void testCalculatePriceWithTierOptionsAndPromo() {
        // Arrange
        Subscription sub = new UserSubscription("Иван Иванов", 2000.0, "FAN_ZONE");
        sub.addOption(new FanZonePass("Fast Track", 500.0));
        sub.addOption(new FanZonePass("VIP Bar", 1500.0));
        sub.setAppliedPromo(new PromoCode("FAN20", 20));

        // Act
        double price = sub.calculateBillingPrice();

        // Assert: 2000 * 1.6 + 500 + 1500 = 5200; скидка 20% -> 4160.0
        assertEquals(4160.0, price, 0.01);
    }

    @Test
    @DisplayName("Корпоративная подписка: цена умножается на количество лицензий")
    void testCorporatePriceWithLicenses() {
        // Arrange
        CorporateSubscription corp = new CorporateSubscription("ООО Вектор", 3000.0, "DANCE_FLOOR", 5);
        corp.addOption(new FanZonePass("Fast Track", 500.0));

        // Act
        double price = corp.calculateBillingPrice();

        // Assert: (3000 * 1.0 + 500) * 5 = 17500.0
        assertEquals(17500.0, price, 0.01);
    }

    @Test
    @DisplayName("Отрицательная цена -> InvalidSubscriptionException")
    void testNegativePriceThrowsInvalidSubscriptionException() {
        // Arrange + Act + Assert: конструктор должен бросить непроверяемое исключение
        assertThrows(InvalidSubscriptionException.class,
                () -> new UserSubscription("Пётр Петров", -100.0, "FAN_ZONE"));
    }

    @Test
    @DisplayName("Оплата несуществующего email -> SubscriptionNotFoundException")
    void testPaymentForUnknownEmailThrowsNotFoundException() {
        // Arrange: база пуста (setUp)

        // Act + Assert
        assertThrows(SubscriptionNotFoundException.class,
                () -> billing.processPaymentByEmail("ghost@concert.ru"));
    }

    @Test
    @DisplayName("Повторная регистрация email -> DuplicateSubscriptionException")
    void testDuplicateEmailThrowsDuplicateSubscriptionException() throws DuplicateSubscriptionException {
        // Arrange
        billing.registerSubscription("ivanov@mail.ru", new UserSubscription("Иван Иванов", 2000.0, "FAN_ZONE"));

        // Act + Assert: тот же email второй раз
        assertThrows(DuplicateSubscriptionException.class,
                () -> billing.registerSubscription("ivanov@mail.ru",
                        new UserSubscription("Двойник Иванов", 1500.0, "DANCE_FLOOR")));
    }

    @Test
    @DisplayName("Stream API: findVipClients возвращает только чеки выше порога")
    void testFindVipClientsFiltersByThreshold() throws DuplicateSubscriptionException {
        // Arrange
        fillDatabaseWithKnownSubscriptions();

        // Act
        List<Subscription> vipClients = billing.findVipClients(5000.0);

        // Assert: из 1000 / 3200 / 10500 порог 5000 проходит только 10500
        assertEquals(1, vipClients.size());
        assertEquals("Виктор ВИП", vipClients.get(0).getOwnerName());
        assertTrue(vipClients.stream().allMatch(s -> s.calculateBillingPrice() > 5000.0));
    }

    @Test
    @DisplayName("Stream API: calculateTotalRevenue суммирует чеки всей базы")
    void testCalculateTotalRevenue() throws DuplicateSubscriptionException {
        // Arrange
        fillDatabaseWithKnownSubscriptions();

        // Act
        double revenue = billing.calculateTotalRevenue();

        // Assert: 1000 + 3200 + 10500 = 14700.0
        assertEquals(14700.0, revenue, 0.01);
    }

    @Test
    @DisplayName("Stream API: countByTier считает тариф без учёта регистра")
    void testCountByTierIgnoreCase() throws DuplicateSubscriptionException {
        // Arrange
        fillDatabaseWithKnownSubscriptions();

        // Act + Assert
        assertEquals(1L, billing.countByTier("VIP_LOGE"));
        assertEquals(1L, billing.countByTier("vip_loge")); // нижний регистр — equalsIgnoreCase
        assertEquals(0L, billing.countByTier("PREMIUM"));  // несуществующий тариф
    }

    @Test
    @DisplayName("Lombok @SuperBuilder: объект валиден, список опций пуст (@Builder.Default)")
    void testSuperBuilderCreatesSubscriptionWithEmptyOptions() {
        // Arrange + Act
        Subscription sub = UserSubscription.builder()
                .ownerName("Билдер Тестов")
                .basePrice(1000.0)
                .tier("DANCE_FLOOR")
                .build();

        // Assert: @Builder.Default инициализировал список, иначе был бы null и NPE
        assertNotNull(sub.getAppliedOptions());
        assertTrue(sub.getAppliedOptions().isEmpty());
        assertEquals(1000.0, sub.calculateBillingPrice(), 0.01);
    }
}