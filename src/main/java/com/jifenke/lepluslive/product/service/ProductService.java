package com.jifenke.lepluslive.product.service;

import com.jifenke.lepluslive.order.domain.entities.OnLineOrder;
import com.jifenke.lepluslive.order.domain.entities.OrderDetail;
import com.jifenke.lepluslive.product.domain.entities.Product;
import com.jifenke.lepluslive.product.domain.entities.ProductDetail;
import com.jifenke.lepluslive.product.domain.entities.ProductSpec;
import com.jifenke.lepluslive.product.domain.entities.ProductType;
import com.jifenke.lepluslive.product.repository.ProductDetailRepository;
import com.jifenke.lepluslive.product.repository.ProductRepository;
import com.jifenke.lepluslive.product.repository.ProductSpecRepository;
import com.jifenke.lepluslive.product.repository.ProductTypeRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;


/**
 * Created by wcg on 16/3/9.
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

  @Inject
  private ProductRepository productRepository;

  @Inject
  private ProductTypeRepository productTypeRepository;

  @Inject
  private ProductSpecRepository productSpecRepository;

  @Inject
  private ProductDetailRepository productDetailRepository;

  @Inject
  private EntityManager em;

  /**
   * 支付成功后修改订单中product的销售量 16/09/27
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  public void editProductSaleByPayOrder(OnLineOrder order) throws Exception {
    List<OrderDetail> list = order.getOrderDetails();
    try {
      for (OrderDetail detail : list) {
        Product product = detail.getProduct();
        product
            .setSaleNumber((product.getSaleNumber() == null ? 0 : product.getSaleNumber()) + detail
                .getProductNumber());
        productRepository.save(product);
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception();
    }
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<Product> findProductsByPage(Integer offset, Integer productType) {
    if (offset == null) {
      offset = 1;
    }

    Page<Product>
        page =
        productRepository.findByStateAndProductTypeAndType(
            new PageRequest(offset - 1, 10, new Sort(Sort.Direction.ASC, "sid")), 1,
            new ProductType(
                productType), 1);
    return page.getContent();
  }


  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public Product findOneProduct(Long id) {
    return productRepository.findOne(id);
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<ProductSpec> findAllProductSpec(Product product) {
    return productSpecRepository.findAllByProductAndState(product, 1);
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public Long getTotalCount() {
    return productRepository.getTotalCount();
  }


  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<ProductType> findAllProductType() {
    return productTypeRepository.findAll();
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public ProductType findOneProductType(Integer id) {
    return productTypeRepository.findOne(id);
  }


  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<ProductDetail> findAllProductDetailsByProduct(Product product) {
    return productDetailRepository.findAllByProductOrderBySid(product);
  }

  public List<Product> findProducts() {
    return productRepository.findAll();
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  public ProductSpec editProductSpecRepository(Long productSpecId, int productNum) {
    ProductSpec productSpec = productSpecRepository.findOne(productSpecId);
    Integer repository = productSpec.getRepository() - productNum;
    if (repository >= 0) {
      productSpec.setRepository(repository);
      productSpecRepository.save(productSpec);
      return productSpec;
    }

    return null;
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  public void orderCancle(List<OrderDetail> orderDetails) {
    for (OrderDetail orderDetail : orderDetails) {
      ProductSpec productSpec = orderDetail.getProductSpec();
      Integer repository = productSpec.getRepository() + orderDetail.getProductNumber();
      productSpec.setRepository(repository);
      productSpecRepository.save(productSpec);
    }

  }

  public ProductSpec findOneProductSpec(Long productSpec) {
    return productSpecRepository.findOne(productSpec);
  }

  /**
   * 获取主打爆品 16/09/21
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public Map findMainHotProduct() {
    List<Object[]> list = productRepository.findMainHotProductList();
    if (list != null && list.size() > 0) {
      Object[] o = list.get(0);
      Map<String, Object> map = convertData(o);
      map.put("repository", o[12]);
      return map;
    }
    return null;
  }

  /**
   * 分页获取爆品列表 16/09/21
   *
   * @param currPage 第几页
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<Map> findHotProductListByPage(Integer currPage) {
    List<Map> mapList = new ArrayList<>();
    List<Object[]> list = productRepository.findHotProductListByPage((currPage - 1) * 10, 10);
    if (list != null && list.size() > 0) {
      for (Object[] o : list) {
        Map<String, Object> map = convertData(o);
        map.put("repository", o[12]);
        mapList.add(map);
      }
      return mapList;
    }
    return null;
  }

  /**
   * 分页获取臻品列表 16/09/21
   *
   * @param currPage 第几页
   * @param typeId   臻品类型  0=所有类型
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<Map> findProductListByTypeAndPage(Integer currPage, Integer typeId) {
    List<Map> mapList = new ArrayList<>();
    List<Object[]> list = null;
    if (typeId == 0) { //不分类
      list = productRepository.findProductListByPage((currPage - 1) * 10, 10);
    } else { //分类
      list = productRepository.findProductListByTypeAndPage(typeId, (currPage - 1) * 10, 10);
    }
    if (list != null && list.size() > 0) {
      for (Object[] o : list) {
        Map<String, Object> map = convertData(o);
        map.put("repository", o[12]);
        mapList.add(map);
      }
      return mapList;
    }
    return null;
  }

  private Map<String, Object> convertData(Object[] o) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", o[0]);
    map.put("name", o[1]);
    map.put("price", o[2]);
    map.put("minPrice", o[3]);
    map.put("minScore", o[4]);
    map.put("picture", o[5]);
    map.put("thumb", o[6]);
    map.put("saleNumber", (int) o[7] + (int) o[8]);
    map.put("hotStyle", o[9]);
    map.put("postage", o[10]);
    map.put("buyLimit", o[11]);
    return map;
  }



  /********************************************* 秒杀相关 ************************************************/

  /**
   * app端  秒杀时段
   * @param sec_kill_date
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<Map<String, Object>> productSecKill_time_list(String sec_kill_date) {

    String sql = null;
    sql = " SELECT id, sec_kill_date, sec_kill_date_name, start_time, end_time, time_limit_number"
          + " FROM product_sec_kill_time "
          + " WHERE sec_kill_date >= '" + sec_kill_date + "'";
    sql +=  " ORDER BY sec_kill_date ASC, start_time ASC ";
    sql +=  " LIMIT " + 0 + "," + 3;

    Query query = em.createNativeQuery(sql);
    List<Object[]> list = query.getResultList();

    List<Map<String, Object>> result = new ArrayList<>();

    if (list.size() > 0){
      for (Object[] o2 : list) {
        Map<String, Object> map = new HashMap<>();
        map.put("sk_time_id",     o2[0] == null ? "0" : o2[0].toString());

        String sk_date = "";
        if (o2[1] != null){
          sk_date = o2[1].toString();
        }
        map.put("sk_date",        sk_date);
        map.put("sk_date_name",   o2[2] == null ? "" : o2[2].toString());


        String start_time = "";
        if (o2[3] != null){
          start_time = o2[3].toString();
        }
        Long start_time222 = 0L;
        if (!sk_date.equals("") && !start_time.equals("")){
          start_time222 = getTimestamp(sk_date,start_time);
        }

        String end_time = "";
        if (o2[4] != null){
          end_time = o2[4].toString();
        }
        Long end_time222 = 0L;
        if (!sk_date.equals("") && !end_time.equals("")){
          end_time222 = getTimestamp(sk_date,end_time);
        }
        map.put("start_time",     start_time222);
        map.put("end_time",       end_time222);
        map.put("limit_number",   o2[5] == null ? 0 : o2[5]);
        map.put("current_time",   new Date().getTime());
        result.add(map);
      }
    }
    return result;
  }


  /**
   * app端  秒杀时段对应的秒杀商品
   * @param sk_time_id
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public List<Map> findProductSecKillByTime(Integer sk_time_id) {

    String sql = null;
    sql = " SELECT psk.id, psk.product_sec_kill_time_id, psk.product_id, "
          + " p.picture, p.name, p.min_score, p.min_price, p.price, p.custom_sale, p.sale_num, (p.custom_sale - p.sale_num), psk.link_product_id "
          + " FROM product_sec_kill psk INNER JOIN product p ON psk.product_id = p.id "
          + " WHERE p.state = 1 AND psk.product_sec_kill_time_id = " + sk_time_id;
    sql += " ORDER BY psk.create_time ASC ";
    sql += " LIMIT " + 0 + "," + 100;

    Query query = em.createNativeQuery(sql);
    List<Object[]> list = query.getResultList();

    List<Map> result = new ArrayList<>();

    if (list.size() > 0){
      for (Object[] o2 : list) {
        Map<String, Object> map = new HashMap<>();
        map.put("psk_id",         o2[0] == null ? 0 : o2[0]);
        map.put("product_id",     o2[2] == null ? 0 : o2[2]);
        map.put("picture",        o2[3] == null ? "" : o2[3].toString());
        map.put("name",           o2[4] == null ? "" : o2[4].toString());
        map.put("min_score",      o2[5] == null ? 0 : o2[5]);
        map.put("min_price",      o2[6] == null ? 0 : o2[6]);
        map.put("price",          o2[7] == null ? 0 : o2[7]);
        map.put("custom_sale",    o2[8] == null ? 0 : o2[8]);
        map.put("sale_num",       o2[9] == null ? 0 : o2[9]);
        map.put("remain_num",     o2[10] == null ? 0 : o2[10]);
        map.put("link_product_id",o2[11] == null ? 0 : o2[11]);
        result.add(map);
      }
    }
    return result;
  }


  /**
   * app端  秒杀详情
   * @param psk_id
   * @param product_id
   */
  @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
  public Map<String, Object> findProductSecKillDetail(Integer psk_id, Long product_id) {

    String sql = null;
    sql = " SELECT p.picture, p.name, p.min_score, p.price, p.custom_sale, p.sale_num, (p.custom_sale - p.sale_num), "
          + " p.postage, p.buy_limit, pskt.sec_kill_date, pskt.start_time, pskt.end_time, p.description "
          + " FROM product_sec_kill psk INNER JOIN product p ON psk.product_id = p.id "
          + " INNER JOIN product_sec_kill_time pskt ON pskt.id = psk.product_sec_kill_time_id "
          + " WHERE p.state = 1 AND p.id = " + product_id + " AND psk.id = " + psk_id;
    sql += " ORDER BY psk.create_time ASC ";
    sql += " LIMIT " + 0 + "," + 10;

    Query query = em.createNativeQuery(sql);
    List<Object[]> list = query.getResultList();

    Map<String, Object> map = new HashMap<>();

    if (list.size() > 0){
      Object[] o2 = list.get(0);
//      map.put("picture",          o2[0] == null ? "" : o2[0].toString());
      map.put("name",             o2[1] == null ? "" : o2[1].toString());
      map.put("min_score",        o2[2] == null ? 0 : o2[2]);
      map.put("price",            o2[3] == null ? 0 : o2[3]);
      map.put("custom_sale",      o2[4] == null ? 0 : o2[4]);
      map.put("sale_num",         o2[5] == null ? 0 : o2[5]);
      map.put("remain_num",       o2[6] == null ? 0 : o2[6]);
      map.put("postage",          o2[7] == null ? 0 : o2[7]);
      map.put("buy_limit",        o2[8] == null ? 0 : o2[8]);

      String sk_date = "";
      if (o2[9] != null){
        sk_date = o2[9].toString();
      }
      map.put("sec_kill_date",    sk_date);


      String start_time = "";
      if (o2[10] != null){
        start_time = o2[10].toString();
      }
      Long start_time222 = 0L;
      if (!sk_date.equals("") && !start_time.equals("")){
        start_time222 = getTimestamp(sk_date,start_time);
      }

      String end_time = "";
      if (o2[11] != null){
        end_time = o2[11].toString();
      }
      Long end_time222 = 0L;
      if (!sk_date.equals("") && !end_time.equals("")){
        end_time222 = getTimestamp(sk_date,end_time);
      }
      map.put("start_time",       start_time222);
      map.put("end_time",         end_time222);
      map.put("current_time",     new Date().getTime());
      map.put("subtitle",             o2[12] == null ? "" : o2[12].toString());
    }

    //商品规格
    String sql2 = null;
    sql2 = " SELECT ps.id, ps.spec_detail, ps.repository, ps.product_id "
          + " FROM product_spec ps "
          + " WHERE ps.state = 1 AND ps.product_id = " + product_id;
    sql2 += " LIMIT " + 0 + "," + 100;
    Query query2 = em.createNativeQuery(sql2);
    List<Object[]> list2 = query2.getResultList();

    List<Map> result = new ArrayList<>();
    if (list2.size() > 0){
      for (Object[] o2 : list2) {
        Map<String, Object> map222 = new HashMap<>();
        map222.put("product_spec_id", o2[0] == null ? 0 : o2[0]);
        map222.put("spec_detail",     o2[1] == null ? "" : o2[1].toString());
        map222.put("repository",      o2[2] == null ? 0 : o2[2]);
        map222.put("product_id",      o2[3] == null ? 0 : o2[3]);
        result.add(map222);
      }
    }
    map.put("productSpec",result);

    //轮播图
    String sql3 = null;
    sql3 = " SELECT sp.id, sp.product_id, sp.picture "
           + " FROM scroll_picture sp "
           + " WHERE sp.product_id = " + product_id;
    sql3 += " LIMIT " + 0 + "," + 100;
    Query query3 = em.createNativeQuery(sql3);
    List<Object[]> list3 = query3.getResultList();

    List<Map> result3 = new ArrayList<>();
    if (list3.size() > 0){
      for (Object[] o2 : list3) {
        Map<String, Object> map222 = new HashMap<>();
        map222.put("product_id",      o2[1] == null ? 0 : o2[1]);
        map222.put("picture",         o2[2] == null ? "" : o2[2].toString());
        result3.add(map222);
      }
    }
    map.put("scrollPicture",result3);

    //详情图
    String sql4 = null;
    sql4 = " SELECT pd.id, pd.product_id, pd.picture "
           + " FROM product_detail pd "
           + " WHERE pd.product_id = " + product_id;
    sql4 += " LIMIT " + 0 + "," + 100;
    Query query4 = em.createNativeQuery(sql4);
    List<Object[]> list4 = query4.getResultList();

    List<Map> result4 = new ArrayList<>();
    if (list4.size() > 0){
      for (Object[] o2 : list4) {
        Map<String, Object> map222 = new HashMap<>();
        map222.put("product_id",     o2[1] == null ? 0 : o2[1]);
        map222.put("picture",     o2[2] == null ? "" : o2[2].toString());
        result4.add(map222);
      }
    }
    map.put("productDetail",result4);

    return map;
  }

  //转换时间戳
  public Long getTimestamp(String date, String time) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String dateTime = date + " " + time;
    Long timestamp = 0L;
    try {
      timestamp = sdf.parse(dateTime).getTime();
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return timestamp;
  }

}
