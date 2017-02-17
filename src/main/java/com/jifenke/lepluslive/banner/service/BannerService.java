package com.jifenke.lepluslive.banner.service;

import com.jifenke.lepluslive.banner.domain.criteria.BannerCriteria;
import com.jifenke.lepluslive.banner.domain.entities.Banner;
import com.jifenke.lepluslive.banner.domain.entities.BannerType;
import com.jifenke.lepluslive.banner.repository.BannerRepository;
import com.jifenke.lepluslive.merchant.domain.entities.City;
import com.jifenke.lepluslive.merchant.domain.entities.Merchant;
import com.jifenke.lepluslive.merchant.service.CityService;
import com.jifenke.lepluslive.product.domain.entities.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * app广告轮播 Created by zhangwen on 16/8/26.
 */
@Service
@Transactional(readOnly = true)
public class BannerService {

  @Inject
  private BannerRepository bannerRepository;

  @Inject
  private CityService cityService;

  @Inject
  private EntityManager em;

  /**
   * 首页，臻品轮播，新品首发
   *
   * @param type 轮播图类型
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<Map> findByType123(Integer type) {
    List<Map> mapList = new ArrayList<>();
    List<Object[]> list = bannerRepository.findByType123(type);
    for (Object[] o : list) {
      Map<String, Object> map = new HashMap<>();
      map.put("sid", o[0]);
      map.put("picture", o[1]);
      map.put("afterType", o[2]);
      map.put("url", o[3]);
      map.put("merchantId", o[4]);
      map.put("productId", o[5]);
      map.put("urlTitle", o[6]);
      mapList.add(map);
    }
    return mapList;
  }

  /**
   * 9=首页轮播图,10=首页好店推荐,11=首页臻品推荐
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<Map> findHomePageByType(BannerCriteria bannerCriteria) {
    List<Map> mapList = new ArrayList<>();

    Sort sort = new Sort(Sort.Direction.ASC, "sid");
    Page page = bannerRepository.findAll(getWhereClause(bannerCriteria),
                                         new PageRequest(bannerCriteria.getOffset() - 1,
                                                         bannerCriteria.getPageSize(), sort));
    List<Banner> listb = page.getContent();
    if (listb.size() > 0) {
      for (Banner b : listb) {
        Map<String, Object> map = new HashMap<>();
        map.put("sid", b.getSid());
        map.put("picture", b.getPicture());
        map.put("afterType", b.getAfterType());
        map.put("url", b.getUrl());
        map.put("urlTitle", b.getUrlTitle());
        map.put("introduce", b.getIntroduce());
        Merchant merchant = b.getMerchant();
        if (merchant != null) {
          map.put("merchantId", merchant.getId());
          map.put("merchantName", merchant.getName());
        }
        Product product = b.getProduct();
        if (product != null) {
          map.put("productId", product.getId());
          map.put("productName", product.getName());
          map.put("productPrice", product.getPrice());
          map.put("productPriceTrue", product.getMinPrice());
          map.put("productScore", product.getMinScore());
        }
        mapList.add(map);
      }
    }

    return mapList;

  }

  /**
   * 10=首页好店推荐
   *
   * @param status    是否获取用户经纬度 1=是,0=否
   * @param longitude 经度
   * @param latitude  纬度
   *
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<Map> findHomePageByType10(Integer status, Double longitude, Double latitude) {
    List<Map> mapList = new ArrayList<>();

    String sql = null;
    if (status != null && status == 1 && longitude!=null && latitude!=null) {
      sql =
          "SELECT m.sid b_sid, m.picture b_picture, m.after_type b_after_type, m.url b_url, m.url_title b_url_title, m.introduce b_introduce, m.id merchant_id, m.merchant_name merchant_name, t.`name` AS tName, area.`name` AS aName, m.lj_commission, m.scorearebate, m.scorebrebate, re.import_scorebscale, m.partnership, ROUND( 6378.138 * 2 * ASIN(SQRT(POW(SIN(("
          + latitude + " * PI() / 180 - m.lat * PI() / 180) / 2),2) + COS(" + latitude
          + " * PI() / 180) * COS(m.lat * PI() / 180) * POW(SIN((" + longitude
          + " * PI() / 180 - m.lng * PI() / 180) / 2),2))) * 1000) AS distance "
          + " FROM (SELECT b1.sid sid, b1.picture picture, b1.after_type after_type, b1.url url, b1.url_title url_title, b1.introduce introduce, b1.merchant_id id, m1.name merchant_name, m1.lat lat, m1.lng lng, m1.merchant_type_id merchant_type_id, m1.area_id area_id, m1.lj_commission lj_commission, m1.scorearebate scorearebate, m1.scorebrebate scorebrebate, m1.partnership partnership "
          + "       FROM banner b1 INNER JOIN merchant m1 ON b1.merchant_id = m1.id WHERE b1.banner_type_id = 10 AND b1.status = 1 "
//          + " AND m1.state = 1"
          + " ) m "
          + " INNER JOIN merchant_type t ON m.merchant_type_id = t.id INNER JOIN area ON m.area_id = area.id LEFT OUTER JOIN merchant_rebate_policy re ON m.id = re.merchant_id";
    } else {
      sql =
          "SELECT m.sid b_sid, m.picture b_picture, m.after_type b_after_type, m.url b_url, m.url_title b_url_title, m.introduce b_introduce, m.id merchant_id, m.merchant_name merchant_name, t.`name` AS tName, area.`name` AS aName, m.lj_commission, m.scorearebate, m.scorebrebate, re.import_scorebscale, m.partnership "
          + " FROM (SELECT b1.sid sid, b1.picture picture, b1.after_type after_type, b1.url url, b1.url_title url_title, b1.introduce introduce, b1.merchant_id id, m1.name merchant_name, m1.lat lat, m1.lng lng, m1.merchant_type_id merchant_type_id, m1.area_id area_id, m1.lj_commission lj_commission, m1.scorearebate scorearebate, m1.scorebrebate scorebrebate, m1.partnership partnership "
          + "       FROM banner b1 INNER JOIN merchant m1 ON b1.merchant_id = m1.id WHERE b1.banner_type_id = 10 AND b1.status = 1 "
//          + " AND m1.state = 1 "
          + " ) m "
          + " INNER JOIN merchant_type t ON m.merchant_type_id = t.id INNER JOIN area ON m.area_id = area.id LEFT OUTER JOIN merchant_rebate_policy re ON m.id = re.merchant_id";
    }
    sql += " ORDER BY m.sid ASC";
    sql += " LIMIT " + 0 + "," + 100;

    Query query = em.createNativeQuery(sql);
    List<Object[]> list = query.getResultList();

    for (Object[] o : list) {
      Map<String, Object> map = new HashMap<>();
      map.put("sid", o[0]);
      map.put("picture", o[1]);
      map.put("afterType", o[2]);
      map.put("url", o[3]);
      map.put("urlTitle", o[4]);
      map.put("introduce", o[5]);
      map.put("merchantId", o[6]);
      map.put("merchantName", o[7]);
      map.put("typeName", o[8]);
      map.put("area", o[9]);
      map.put("commission", o[10]);
      map.put("aRebate", o[11]);
      map.put("normalBScale", o[12]);
      map.put("importBScale", o[13]);
      map.put("friend", o[14]);
      if (o.length > 15) {
        map.put("distance", o[15]);
      } else {
        map.put("distance", 0);
      }
      mapList.add(map);
    }
    return mapList;
  }

  private static Specification<Banner> getWhereClause(BannerCriteria criteria) {
    return new Specification<Banner>() {
      @Override
      public Predicate toPredicate(Root<Banner> r, CriteriaQuery<?> q,
                                   CriteriaBuilder cb) {
        Predicate predicate = cb.conjunction();
        if (criteria.getType() != null) {   //banner类型
          predicate.getExpressions().add(
              cb.equal(r.<BannerType>get("bannerType").get("id"), criteria.getType()));
        }
        predicate.getExpressions().add(//1上架、0下架
                                       cb.equal(r.get("status"), 1));

        return predicate;
      }
    };
  }

  /**
   * 当期好店推荐 16/09/14
   *
   * @param cityId 城市Id
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<Map> findByNewShop(Long cityId) {
    List<Map> mapList = new ArrayList<>();
    List<Object[]> list = null;
    City city = null;
    if (cityId != null) {
      city = cityService.findCityById(cityId);
    }
    if (city != null) {
      list = bannerRepository.findNewShopByCity(cityId);
    } else {
      list = bannerRepository.findByNewShop();
    }
    if (list != null) {
      for (Object[] o : list) {
        Map<String, Object> map = new HashMap<>();
        map.put("sid", o[0]);
        map.put("picture", o[1]);
        map.put("url", o[2]);
        map.put("merchantId", o[3]);
        map.put("title", o[4]);
        map.put("content", o[5]);
        map.put("shopName", o[6]);
        map.put("urlTitle", o[7]);
        map.put("afterType", o[8]);
        mapList.add(map);
      }
    }
    return mapList;
  }

  /**
   * 往期好店推荐 16/09/14
   *
   * @param cityId   城市ID
   * @param startNum 起始记录
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<Map> findByOldShop(Long cityId, Integer startNum) {
    List<Map> mapList = new ArrayList<>();
    List<Object[]> list = null;
    City city = null;
    if (cityId != null) {
      city = cityService.findCityById(cityId);
    }
    if (city != null) {
      list = bannerRepository.findOldShopByCity(cityId, (startNum - 1) * 10, 10);
    } else {
      list = bannerRepository.findByOldShop((startNum - 1) * 10, 10);
    }
    if (list != null) {
      for (Object[] o : list) {
        Map<String, Object> map = new HashMap<>();
        map.put("sid", o[0]);
        map.put("picture", o[1]);
        map.put("url", o[2]);
        map.put("merchantId", o[3]);
        map.put("urlTitle", o[4]);
        map.put("afterType", o[5]);
        mapList.add(map);
      }
    }
    return mapList;
  }

  /**
   * 当期好货推荐
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<Map> findByNewProduct() {
    List<Map> mapList = new ArrayList<>();
    List<Object[]> list = bannerRepository.findByNewProduct();
    for (Object[] o : list) {
      Map<String, Object> map = new HashMap<>();
      map.put("sid", o[0]);
      map.put("picture", o[1]);
      map.put("afterType", o[2]);
      map.put("url", o[3]);
      map.put("productId", o[4]);
      map.put("title", o[5]);
      map.put("content", o[6]);
      map.put("price", o[7]);
      map.put("urlTitle", o[8]);
      mapList.add(map);
    }
    return mapList;
  }

  /**
   * 往期好货推荐
   *
   * @param startNum 起始记录
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<Map> findByOldProduct(Integer startNum) {
    List<Map> mapList = new ArrayList<>();
    List<Object[]> list = bannerRepository.findByOldProduct((startNum - 1) * 10, 10);
    for (Object[] o : list) {
      Map<String, Object> map = new HashMap<>();
      map.put("sid", o[0]);
      map.put("picture", o[1]);
      map.put("afterType", o[2]);
      map.put("url", o[3]);
      map.put("productId", o[4]);
      map.put("title", o[5]);
      map.put("content", o[6]);
      map.put("price", o[7]);
      map.put("urlTitle", o[8]);
      mapList.add(map);
    }
    return mapList;
  }

  /**
   * 往期好货推荐
   *
   * @param id 城市id
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public String hotWord(Long id) {
    //获取所在城市
    City city = cityService.findCityById(id);
    if (city == null) {
      city = new City();
      city.setId(1L);
    }
    //获取该城市的热门关键词集合
    List<Banner> list = bannerRepository.findByCityOrderByCreateDateDesc(city);
    String hot = "";
    for (Banner banner : list) {
      hot += banner.getTitle() + "_";
    }
    return hot;
  }

  /**
   * app端   启动广告
   * @param cityId 城市id
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public Map<String, Object> startAd(Long cityId, Integer type) {

    String sql = null;
    sql = " SELECT b1.sid, b1.picture, b1.after_type, b1.url, b1.url_title, b1.product_id, b1.merchant_id"
          + " FROM banner b1 "
//          + " LEFT JOIN city c1 ON b1.city_id = c1.id "
          + " WHERE b1.banner_type_id = 12 AND b1.status = 1 ";
    if (cityId != null && !"null".equals(cityId) && !"".equals(cityId)) {
      sql += " AND b1.city_id = " + cityId;
    }
    sql += " AND b1.app_type = " + type;
    sql += " ORDER BY b1.sid DESC ";
    sql += " LIMIT " + 0 + "," + 10;

    Query query = em.createNativeQuery(sql);
    List<Object[]> list = query.getResultList();

    Map<String, Object> result = new HashMap<>();
    if (list.size() > 0){
      Object[] o2 = list.get(0);
//      result.put("sid",         o2[0] == null ? "0" : o2[0].toString());
      result.put("picture",     o2[1] == null ? "" : o2[1].toString());
      result.put("afterType",   o2[2] == null ? "0" : o2[2].toString());
      result.put("url",         o2[3] == null ? "" : o2[3].toString());
      result.put("urlTitle",    o2[4] == null ? "" : o2[4].toString());
      result.put("productId",   o2[5] == null ? "0" : o2[5].toString());
      result.put("merchantId",  o2[6] == null ? "0" : o2[6].toString());
    }
    return result;
  }

}
