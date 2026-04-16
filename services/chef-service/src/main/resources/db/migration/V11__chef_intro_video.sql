-- Add intro video URL to chef profiles
ALTER TABLE chefs.chef_profiles ADD COLUMN intro_video_url VARCHAR(500);

-- Add media_type to chef_photos to support video entries in gallery
ALTER TABLE chefs.chef_photos ADD COLUMN media_type VARCHAR(10) DEFAULT 'IMAGE';
