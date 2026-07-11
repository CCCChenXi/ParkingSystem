-- ParkingSystem 数据库初始化脚本
-- 数据库: parking_system
-- 字符集: utf8mb4

CREATE DATABASE IF NOT EXISTS parking_system
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE parking_system;

-- 管理员
CREATE TABLE admin (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20),
    create_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户
CREATE TABLE user (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    phone       VARCHAR(20)  UNIQUE,
    avatar      VARCHAR(255),
    create_time DATETIME,
    update_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 车辆
CREATE TABLE vehicle (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    plate_number VARCHAR(20)  NOT NULL,
    brand        VARCHAR(50),
    color        VARCHAR(20),
    create_time  DATETIME,
    FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 停车场
CREATE TABLE parking_lot (
    id              BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)   NOT NULL,
    address         VARCHAR(255),
    longitude       DECIMAL(10,6),
    latitude        DECIMAL(10,6),
    total_spots     INT            DEFAULT 0,
    available_spots INT            DEFAULT 0,
    hourly_rate     DECIMAL(10,2),
    image_url       VARCHAR(500),
    status          INT            DEFAULT 0,
    create_time     DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 车位
CREATE TABLE parking_spot (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lot_id      BIGINT       NOT NULL,
    seq         BIGINT       NOT NULL,
    spot_number VARCHAR(20),
    type        INT          DEFAULT 0,
    create_time DATETIME,
    FOREIGN KEY (lot_id) REFERENCES parking_lot(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单
CREATE TABLE parking_order (
    id           BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no     VARCHAR(64)    NOT NULL,
    user_id      BIGINT         NOT NULL,
    lot_id       BIGINT         NOT NULL,
    spot_id      BIGINT         NOT NULL,
    plate_number VARCHAR(20),
    status       INT            NOT NULL,
    start_time   DATETIME,
    end_time     DATETIME,
    hourly_rate  DECIMAL(10,2),
    amount       DECIMAL(10,2),
    coupon_id    BIGINT,
    discount     DECIMAL(10,2),
    create_time  DATETIME,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (lot_id)  REFERENCES parking_lot(id),
    FOREIGN KEY (spot_id) REFERENCES parking_spot(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 优惠券
CREATE TABLE coupon (
    id              BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)   NOT NULL,
    description     VARCHAR(255),
    discount_amount DECIMAL(10,2),
    min_amount      DECIMAL(10,2),
    type            INT,
    stock           INT,
    remain_stock    INT,
    start_time      DATETIME,
    end_time        DATETIME,
    create_time     DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户-优惠券关联
CREATE TABLE user_coupon (
    id          BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    coupon_id   BIGINT   NOT NULL,
    status      INT,
    create_time DATETIME,
    use_time    DATETIME,
    FOREIGN KEY (user_id)   REFERENCES user(id),
    FOREIGN KEY (coupon_id) REFERENCES coupon(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 钱包
CREATE TABLE wallet (
    id          BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT         NOT NULL UNIQUE,
    balance     DECIMAL(10,2)  DEFAULT 0.00,
    create_time DATETIME,
    update_time DATETIME,
    FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 钱包流水
CREATE TABLE wallet_log (
    id          BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wallet_id   BIGINT         NOT NULL,
    amount      DECIMAL(10,2)  NOT NULL,
    type        INT,
    remark      VARCHAR(255),
    create_time DATETIME,
    FOREIGN KEY (wallet_id) REFERENCES wallet(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息通知
CREATE TABLE message (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(100),
    content     VARCHAR(500),
    type        INT,
    is_read     INT          DEFAULT 0,
    create_time DATETIME,
    FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
