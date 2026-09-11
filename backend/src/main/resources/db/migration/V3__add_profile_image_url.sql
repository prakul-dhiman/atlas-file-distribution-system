-- V3: Add profile image URL column to users table

ALTER TABLE users ADD COLUMN profile_image_url VARCHAR(512);