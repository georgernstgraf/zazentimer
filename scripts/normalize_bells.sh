#!/bin/bash
# Normalizes all files in app/src/main/res/raw/ to -16.0 LUFS
# Dependencies: ffmpeg

for f in app/src/main/res/raw/*.mp3; do
	echo "Normalizing $f..."
	# Create temporary file with .mp3 extension
	tmp=$(mktemp --suffix=.mp3)
	# Apply loudnorm filter to map to -16.0 LUFS, force mp3 output
	ffmpeg -y -i "$f" -af loudnorm=I=-16:TP=-1.5:LRA=11 -ar 44100 -b:a 192k -f mp3 "$tmp"
	# Replace the original file only if normalization succeeded
	if [ $? -eq 0 ]; then
		mv "$tmp" "$f"
	else
		echo "Normalization failed for $f"
		rm "$tmp"
	fi
done
echo "Normalization complete."
