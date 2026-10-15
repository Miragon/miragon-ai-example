-- The bike catalogue (bike_portfolio, seeded on start-up) is not part of the core process: a leasing
-- application references its bike by bike_id only, and the dealer decides availability. V1 stays as
-- it was released; this migration removes the table it created. Bringing the catalogue back is the
-- "bike catalogue" feature issue.

DROP TABLE bike_portfolio;
