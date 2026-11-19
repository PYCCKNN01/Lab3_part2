package model;

import lombok.ToString;
import lombok.experimental.SuperBuilder;

@ToString(callSuper = true)
@SuperBuilder
public class UserSubscription extends Subscription {

    public UserSubscription(String ownerName, double basePrice, String tier) {
        super(ownerName, basePrice, tier);
    }

    @Override
    public double calculateBillingPrice() {
        // 1. Базовая цена с учетом тарифа + сумма всех подключенных пропусков
        double totalPrice = (getBasePrice() * getTierCoefficient()) + calculateOptionsTotal();

        // 2. Применяем скидку по промокоду, если он есть
        if (getAppliedPromo() != null) {
            double discount = getAppliedPromo().getDiscountPercent() / 100.0;
            totalPrice *= (1.0 - discount);
        }

        return Math.round(totalPrice * 100.0) / 100.0;
    }
}

/*
package model;


public class UserSubscription extends Subscription {

    public UserSubscription(String ownerName, double basePrice, String tier) {
        super(ownerName, basePrice, tier);
    }

    @Override
    public double calculateBillingPrice() {
        //Базовая цена с учетом тарифа
        double totalPrice = getBasePrice() * getTierCoefficient();

        //Проходим по всем добавленным пропускам
        for (FanZonePass option : getAppliedOptions()) {
            totalPrice += option.getPrice();
        }

        return totalPrice;
    }
}
 */