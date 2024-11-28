## The application can perform the following actions:
- get a list of currencies used in the project;
- get the exchange rates for the currency;
- Add a new currency to receive exchange rates.
- schedule an update of exchange rates (configurable via scheduler.update-rates.interval).

---

## API Endpoints

The application provides the following REST endpoints. API documentation is available via Swagger.

Swagger UI
Visit the Swagger UI at:
http://localhost:8080/swagger-ui/index.html
Key Endpoints
- GET /api/v1/currencies: Get a list of all available currencies.
- POST /api/v1/currencies: Add a new currency to the system.
- GET /api/v1/exchange-rates/{baseCurrency}: Retrieve exchange rates for a specified base currency.
