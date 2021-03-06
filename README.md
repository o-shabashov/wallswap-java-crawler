# Wallswap-Java

[![BCH compliance](https://bettercodehub.com/edge/badge/o-shabashov/wallswap-java-crawler?branch=master)](https://bettercodehub.com/)

The project is just for fun and test programming skills. It consists of two parts - the server and crawler.

### Server:
See [https://github.com/o-shabashov/wallswap-java](https://github.com/o-shabashov/wallswap-java)

### Crawler:
1. Parses website [https://wallhaven.cc](https://wallhaven.cc) and collects direct URL's at the wallpaper;
2. Saves list of wallpapers in the database;
3. Upload wallpapers for each user in Dropbox directory.

## Installation
* Create MySQL database `wallswap` and import `wallswap.sql`
* Install gradle dependencies:
```bash
cd wallswap-java-crawler
gradle build
```
* Run crawl once a week:
```bash
gradle build ; java -jar build/libs/wallswap-java-crawler-0.1.0.jar
```

## Made with
1. [Jsoup](https://jsoup.org/)
2. MySQL
3. [Dropbox SDK](https://github.com/dropbox/dropbox-sdk-java)
