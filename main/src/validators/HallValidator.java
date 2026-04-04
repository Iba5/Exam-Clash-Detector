package src.validators;

import src.models.Hall;

public class HallValidator {

    public static void validate(Hall hall) {

        Validator.requireNonNull(hall, "Hall cannot be null");

        Validator.requirePositive(hall.getHallId(), "Hall ID must be positive");

        Validator.requireNonEmpty(hall.getHallName(), "Hall name required");

        Validator.requirePositive(
                hall.getSeatingCapacity(),
                "Seating capacity must be greater than 0"
        );
    }
}

