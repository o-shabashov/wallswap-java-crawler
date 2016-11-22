package com.oshabashov.wallswap.crawl;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Should be run once a week
 */
public class Crawl {
  private Connection connection;

  public Crawl() {
    String myDriver = "org.gjt.mm.mysql.Driver";
    String myUrl    = "jdbc:mysql://localhost/wallswap?autoReconnect=true&useSSL=false";
    try {
      Class.forName(myDriver);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    try {
      this.connection = DriverManager.getConnection(myUrl, "root", "root");
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
    Crawl crawl = new Crawl();

    // Truncate Database
    String    query = "truncate wallpaper";
    Statement st    = crawl.connection.createStatement();
    st.executeUpdate(query);

    // Parse HTML and get List of wallpapers
    List<Wallpaper> wallpapers = crawl.parseHtml();

    // Store to DB
    crawl.storeInDb(wallpapers);

    // Upload wallpapers for each user, using access token
    query = "SELECT access_token FROM `user`";
    st = crawl.connection.createStatement();
    ResultSet rs = st.executeQuery(query);
    while (rs.next()) {
      try {
        crawl.upload(rs.getString("access_token"), wallpapers);
      } catch (DbxException e) {
        e.printStackTrace();
      }
    }
    st.close();
  }

  public List<Wallpaper> parseHtml() throws SQLException {
    Document        doc        = null;
    String          thumbUrl, url, wallpaperId;
    List<Wallpaper> wallpapers = new ArrayList<>();

    try {
      doc = Jsoup.connect("https://alpha.wallhaven.cc/search?categories=101&purity=100&sorting=random&order=desc")
        .userAgent("Mozilla")
        .get();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    if (doc != null) {
      Elements figures = doc.select("figure");
      for (Element element : figures) {
        wallpaperId = element.attr("data-wallpaper-id");
        System.out.println(wallpaperId);
        thumbUrl = "https://alpha.wallhaven.cc/wallpapers/thumb/small/th-" + wallpaperId + ".jpg";
        url = "https://wallpapers.wallhaven.cc/wallpapers/full/wallhaven-" + wallpaperId + ".jpg";
        wallpapers.add(new Wallpaper(thumbUrl, url));
      }
    }

    return wallpapers;
  }

  public void storeInDb(List<Wallpaper> wallpapers) throws SQLException {
    String            query = "INSERT INTO wallpaper (thumb_url, url) VALUES (?, ?)";
    PreparedStatement preparedStmt;

    for (Wallpaper wallpaper : wallpapers) {
      preparedStmt = this.connection.prepareStatement(query);
      preparedStmt.setString(1, wallpaper.getThumb_url());
      preparedStmt.setString(2, wallpaper.getUrl());
      preparedStmt.executeUpdate();
    }
  }

  public void upload(String accessToken, List<Wallpaper> wallpapers) throws DbxException, IOException {
    DbxClientV2   dbxClient = new DbxClientV2(new DbxRequestConfig("Wallswap/0.1"), accessToken);
    String        fullTargetPath;
    URL           url;
    InputStream   inputStream;
    URLConnection urlConnection;

    dbxClient.files().delete("/wallpapers");

    for (Wallpaper wallpaper : wallpapers) {
      fullTargetPath = "/wallpapers/" + wallpaper.getUrl().substring(wallpaper.getUrl().lastIndexOf('/') + 1, wallpaper.getUrl().length());
      try {
        url = new URL(wallpaper.getUrl());
        urlConnection = url.openConnection();
        urlConnection.addRequestProperty("User-Agent", "Mozilla");
        urlConnection.addRequestProperty("Cookie", "");
        urlConnection.connect();
        inputStream = urlConnection.getInputStream();

        dbxClient.files().upload(fullTargetPath).uploadAndFinish(inputStream);
      } catch (DbxException | IOException e) {
        e.printStackTrace();
      }
    }

  }
}
