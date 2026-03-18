-- Flash sale schema for VoltDB testcontainer integration tests.
-- Models a limited-stock flash sale: products with finite inventory,
-- where Purchase is an ACID transaction preventing overselling.

CREATE TABLE products (
    product_id   INTEGER NOT NULL,
    product_name VARCHAR(100),
    price        DECIMAL(10,2),
    stock_quantity INTEGER NOT NULL,
    CONSTRAINT pk_product PRIMARY KEY (product_id)
);
PARTITION TABLE products ON COLUMN product_id;

-- Tracks individual purchase transactions
CREATE TABLE purchases (
    purchase_id BIGINT    NOT NULL,
    customer_id BIGINT    NOT NULL,
    product_id  INTEGER   NOT NULL,
    quantity    INTEGER   NOT NULL,
    CONSTRAINT pk_purchase PRIMARY KEY (product_id, purchase_id)
);
PARTITION TABLE purchases ON COLUMN product_id;

-- Table used by the external-library (commons-lang3) test
CREATE TABLE string_transform (
    input  VARCHAR(512),
    result VARCHAR(512)
);

-- Declare procedures
CREATE PROCEDURE PARTITION ON TABLE products COLUMN product_id PARAMETER 0 FROM CLASS flashsale.procedures.AddProduct;
CREATE PROCEDURE PARTITION ON TABLE products COLUMN product_id PARAMETER 1 FROM CLASS flashsale.procedures.Purchase;
CREATE PROCEDURE PARTITION ON TABLE products COLUMN product_id PARAMETER 0 FROM CLASS flashsale.procedures.GetStock;
CREATE PROCEDURE FROM CLASS flashsale.procedures.ToJson;
