USE HotelDB;
GO

-- 1. Create tables
IF OBJECT_ID('RoomTypes', 'U') IS NULL
BEGIN
    CREATE TABLE RoomTypes (
        id INT IDENTITY(1,1) PRIMARY KEY,
        name VARCHAR(255) NOT NULL UNIQUE,
        description TEXT
    );
END
GO

IF OBJECT_ID('Amenities', 'U') IS NULL
BEGIN
    CREATE TABLE Amenities (
        id INT IDENTITY(1,1) PRIMARY KEY,
        name VARCHAR(255) NOT NULL UNIQUE,
        icon_code VARCHAR(100)
    );
END
GO

-- 2. Insert data
IF NOT EXISTS (SELECT 1 FROM RoomTypes WHERE name = 'STANDARD')
    INSERT INTO RoomTypes (name, description) VALUES ('STANDARD', 'Phòng tiêu chuẩn với tiện nghi cơ bản');
IF NOT EXISTS (SELECT 1 FROM RoomTypes WHERE name = 'DELUXE')
    INSERT INTO RoomTypes (name, description) VALUES ('DELUXE', 'Phòng cao cấp với view đẹp');
IF NOT EXISTS (SELECT 1 FROM RoomTypes WHERE name = 'SUITE')
    INSERT INTO RoomTypes (name, description) VALUES ('SUITE', 'Phòng thượng hạng không gian rộng rãi');

IF NOT EXISTS (SELECT 1 FROM Amenities WHERE name = 'Wi-Fi miễn phí')
    INSERT INTO Amenities (name, icon_code) VALUES ('Wi-Fi miễn phí', 'fa-wifi');
IF NOT EXISTS (SELECT 1 FROM Amenities WHERE name = 'Hồ bơi')
    INSERT INTO Amenities (name, icon_code) VALUES ('Hồ bơi', 'fa-swimming-pool');
IF NOT EXISTS (SELECT 1 FROM Amenities WHERE name = 'Bữa sáng miễn phí')
    INSERT INTO Amenities (name, icon_code) VALUES ('Bữa sáng miễn phí', 'fa-coffee');
GO

-- 3. Add constraint to Rooms
IF COL_LENGTH('Rooms', 'room_type_id') IS NULL
BEGIN
    ALTER TABLE Rooms ADD room_type_id INT;
END
GO

-- 4. Update References
UPDATE Rooms SET room_type_id = (SELECT id FROM RoomTypes WHERE name = Rooms.room_type);
GO

-- Default any NULLs just in case
UPDATE Rooms SET room_type_id = 1 WHERE room_type_id IS NULL;
GO

-- Make room_type_id not null
ALTER TABLE Rooms ALTER COLUMN room_type_id INT NOT NULL;
GO

-- Drop old constraints depending on room_type if any. E.g check constraints. 
DECLARE @constraint_name sysname;
SELECT @constraint_name = Object_name(default_object_id) FROM sys.columns WHERE object_id = Object_id('Rooms') AND name = 'room_type';
IF @constraint_name IS NOT NULL
BEGIN
    EXEC('ALTER TABLE Rooms DROP CONSTRAINT ' + @constraint_name);
END
GO

-- Drop old column.
IF COL_LENGTH('Rooms', 'room_type') IS NOT NULL
BEGIN
    ALTER TABLE Rooms DROP COLUMN room_type;
END
GO

-- 5. Add FK
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_Rooms_RoomTypes')
BEGIN
    ALTER TABLE Rooms ADD CONSTRAINT FK_Rooms_RoomTypes FOREIGN KEY (room_type_id) REFERENCES RoomTypes(id);
END
GO

-- 6. Create RoomAmenities join table
IF OBJECT_ID('room_amenities', 'U') IS NULL
BEGIN
    CREATE TABLE room_amenities (
        room_id INT NOT NULL,
        amenity_id INT NOT NULL,
        PRIMARY KEY (room_id, amenity_id),
        CONSTRAINT FK_RoomAmenities_Room FOREIGN KEY (room_id) REFERENCES Rooms(id),
        CONSTRAINT FK_RoomAmenities_Amenity FOREIGN KEY (amenity_id) REFERENCES Amenities(id)
    );
END
GO
