const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const svgPath = 'app/store_logo.svg';
const densities = [
  { name: 'mdpi', size: 48 },
  { name: 'hdpi', size: 72 },
  { name: 'xhdpi', size: 96 },
  { name: 'xxhdpi', size: 144 },
  { name: 'xxxhdpi', size: 192 }
];

async function run() {
  for (const { name, size } of densities) {
    const dir = `app/src/main/res/mipmap-${name}`;
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }

    // Clean up template webp files to avoid conflicts
    const webpFile = path.join(dir, 'ic_launcher.webp');
    const roundWebpFile = path.join(dir, 'ic_launcher_round.webp');
    if (fs.existsSync(webpFile)) {
      fs.unlinkSync(webpFile);
    }
    if (fs.existsSync(roundWebpFile)) {
      fs.unlinkSync(roundWebpFile);
    }

    // Resize SVG to 44% of total icon size (Standard safe zone scaling for square illustrations)
    const svgSize = Math.round(size * 0.44);
    const svgResized = await sharp(svgPath)
      .resize(svgSize, svgSize)
      .toBuffer();

    // 1. Square Launcher Icon (Rounded Rectangle on #FFFFFF)
    const r = Math.round(size * 0.15); // Standard 15% corner radius
    const roundedMask = Buffer.from(
      `<svg><rect x="0" y="0" width="${size}" height="${size}" rx="${r}" ry="${r}" fill="#FFFFFF"/></svg>`
    );

    await sharp({
      create: {
        width: size,
        height: size,
        channels: 4,
        background: { r: 255, g: 255, b: 255, alpha: 1 } // #FFFFFF
      }
    })
      .composite([
        { input: roundedMask, blend: 'dest-in' },
        { input: svgResized, gravity: 'center' }
      ])
      .png()
      .toFile(path.join(dir, 'ic_launcher.png'));

    // 2. Round Launcher Icon (Perfect Circle on #FFFFFF)
    const circleMask = Buffer.from(
      `<svg><circle cx="${size/2}" cy="${size/2}" r="${size/2}" fill="#FFFFFF"/></svg>`
    );

    await sharp({
      create: {
        width: size,
        height: size,
        channels: 4,
        background: { r: 255, g: 255, b: 255, alpha: 1 } // #FFFFFF
      }
    })
      .composite([
        { input: circleMask, blend: 'dest-in' },
        { input: svgResized, gravity: 'center' }
      ])
      .png()
      .toFile(path.join(dir, 'ic_launcher_round.png'));
  }
  console.log('Raster icons generated successfully!');
}

run().catch(console.error);
