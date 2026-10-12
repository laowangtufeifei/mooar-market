package com.mooar.market.mapper;

import com.mooar.market.service.ItemTraitService;

import java.util.List;
import java.util.Map;

public class OrderSqlProvider {

    /**
     * 在售挂单连表查询：721 的属性/tokenId 条件作用在 nft 上，1155 挂单 nft_id=0（连表为空但保留）。
     * 排序按价格由小到大（可选倒序），同价再按 tokenId、挂单 id 兜底。
     */
    @SuppressWarnings("unchecked")
    public String searchOrders(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(
                "SELECT o.id AS order_id, o.item_id, o.seller_id, o.nft_id, o.amount, o.price, " +
                        "n.token_id AS token_id, n.image AS nft_image " +
                        "FROM `order` o LEFT JOIN nft n ON n.id = o.nft_id " +
                        "WHERE o.item_id = #{itemId} AND o.state = #{activeState}");
        if (params.get("tokenIdLike") != null) {
            sb.append(" AND CAST(n.token_id AS CHAR) LIKE CONCAT('%', #{tokenIdLike}, '%')");
        }
        SqlCondBuilder.append(sb, (List<ItemTraitService.TraitCond>) params.get("conds"), "n");
        String dir = Boolean.TRUE.equals(params.get("desc")) ? "DESC" : "ASC";
        sb.append(" ORDER BY o.price ").append(dir).append(", IFNULL(n.token_id, 0) ASC, o.id ASC");
        sb.append(" LIMIT #{offset}, #{limit}");
        return sb.toString();
    }
}
