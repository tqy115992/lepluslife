package com.jifenke.lepluslive.product.controller;


import com.jifenke.lepluslive.global.config.Constants;
import com.jifenke.lepluslive.global.service.MessageService;
import com.jifenke.lepluslive.global.util.CookieUtils;
import com.jifenke.lepluslive.global.util.LejiaResult;
import com.jifenke.lepluslive.order.domain.entities.OnLineOrder;
import com.jifenke.lepluslive.order.service.OnlineOrderService;
import com.jifenke.lepluslive.order.service.OrderService;
import com.jifenke.lepluslive.product.controller.dto.ProductDto;
import com.jifenke.lepluslive.product.domain.entities.Product;
import com.jifenke.lepluslive.product.domain.entities.ProductType;
import com.jifenke.lepluslive.product.domain.entities.ScrollPicture;
import com.jifenke.lepluslive.product.service.ProductService;
import com.jifenke.lepluslive.product.service.ScrollPictureService;
import com.jifenke.lepluslive.weixin.service.DictionaryService;
import com.jifenke.lepluslive.weixin.service.WeiXinPayService;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import io.swagger.annotations.ApiOperation;

/**
 * Created by tqy on 2017/2/8.
 */
@Controller
@RequestMapping("productSecKill")
public class ProductSecKillController {

  @Inject
  private ProductService productService;

  @Inject
  private ScrollPictureService scrollPictureService;

  @Inject
  private DictionaryService dictionaryService;
  @Inject
  private MessageService messageService;
  @Inject
  private OnlineOrderService onlineOrderService;
  @Inject
  private OrderService orderService;
  @Inject
  private WeiXinPayService weiXinPayService;



  @ApiOperation(value = "积分秒杀时段")
  @RequestMapping(value = "/secKillTimes", method = RequestMethod.POST)
  public
  @ResponseBody
  LejiaResult productSecKill_time_list() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date date1 = new Date();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date1);
    calendar.add(Calendar.DAY_OF_YEAR, 0);
    String day1 = sdf.format(calendar.getTime());//今天

    //查询从今天开始的,前三个时段
    List<Map<String, Object>> result = productService.productSecKill_time_list(day1);
    return LejiaResult.ok(result);
  }


  @ApiOperation(value = "根据秒杀时段,查询秒杀商品")
  @RequestMapping(value = "/productByTime", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
  public
  @ResponseBody
  LejiaResult findProductSecKillByTime(@RequestParam(value="sk_time_id", required = true) Integer sk_time_id) {

    List<Map> result = productService.findProductSecKillByTime(sk_time_id);
    return LejiaResult.ok(result);
  }


  @ApiOperation(value = "秒杀商品详情")
  @RequestMapping(value = "/productDetail", method = RequestMethod.POST)
  public
  @ResponseBody
  LejiaResult getProductDetail(@RequestParam(required = true) Integer psk_id,
                               @RequestParam(required = true) Long product_id) {
    Map<String, Object> result = productService.findProductSecKillDetail(psk_id, product_id);
    return LejiaResult.ok(result);
  }


  @ApiOperation(value = "积分秒杀支付接口")
  @RequestMapping(value = "/secKillPay", method = RequestMethod.POST)
  public
  @ResponseBody
  LejiaResult secKillPay(@RequestParam Long orderId, @RequestParam Long truePrice,
                        @RequestParam Long trueScore, @RequestParam Integer transmitWay,
                        HttpServletRequest request) {
    if (orderId == null || truePrice == null || trueScore == null) {
      return LejiaResult.build(5010, messageService.getMsg("5010"));
    }
    try {

      if (truePrice == 0L) {//------------全积分支付-------------

        Map map = onlineOrderService.orderPayByScoreB(orderId, trueScore, transmitWay, 9L);
        String status = "" + map.get("status");
        if (!"200".equals(status)) {
          return LejiaResult.build(Integer.valueOf(status), messageService.getMsg(status));
        }
        return LejiaResult.build((Integer) map.get("status"), "", map.get("data"));

      }else {//-----------用户积分不足----需要微信支付-----------

        Map<Object, Object>
            result =
            orderService.setPriceScoreForOrder(orderId, truePrice, trueScore, transmitWay);
        String status222 = "" + result.get("status");
        if (!"200".equals(status222)) {
          return LejiaResult.build(Integer.valueOf(status222), messageService.getMsg(status222));
        }

        //封装订单参数
        SortedMap<Object, Object>
            map222 =
            weiXinPayService._buildOrderParams(request, (OnLineOrder) result.get("data"));
        //获取预支付id
        Map unifiedOrder = weiXinPayService.createUnifiedOrder(map222);
        if (unifiedOrder.get("prepay_id") != null) {
          SortedMap sortedMap = weiXinPayService.buildAppParams(
              unifiedOrder.get("prepay_id").toString());
          return LejiaResult.build(200, "ok", sortedMap);
        } else {
          return LejiaResult.build(4001, messageService.getMsg("4001"));
        }

      }

    } catch (Exception e) {
      return LejiaResult.build(500, messageService.getMsg("500"));
    }

  }

}
