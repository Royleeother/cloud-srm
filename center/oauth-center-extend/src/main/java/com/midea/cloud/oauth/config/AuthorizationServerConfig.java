package com.midea.cloud.oauth.config;

import com.midea.cloud.oauth.sec.AuthResponseExceptionTranslator;
import com.midea.cloud.oauth.sec.CustomAuthorizationServerSecurityConfiguration;
import com.midea.cloud.oauth.sec.CustomTokenConverter;
import com.midea.cloud.oauth.service.impl.RedisAuthorizationCodeServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * ?????????????????????
 *
 * @author artifact
 */
@Configuration
//@EnableAuthorizationServer
@Import({AuthorizationServerEndpointsConfiguration.class, CustomAuthorizationServerSecurityConfiguration.class})
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    /**
     * ???????????????
     *
     * @see SecurityConfig ???authenticationManagerBean()
     */
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private AuthResponseExceptionTranslator authResponseExceptionTranslator;
    @Resource(name = "customTokenConverter")
    private CustomTokenConverter customTokenConverter;

    /**
     * ??????jwt??????redis
     */
    @Value("${access_token.store-jwt:false}")
    private boolean storeWithJwt;
    /**
     * ??????jwt??????redis
     */
    @Value("${access_token.jwt-sign-key:cloud_sign_key}")
    private String jwtSigningKey;
    /**
     * ???????????????????????????accessToken
     */
    @Value("${access_token.allow-uniqe:false}")
    private boolean allowUniqeToken;
    @Autowired
    private RedisAuthorizationCodeServices redisAuthorizationCodeServices;

    /**
     * ????????????
     *
     * @return
     */
    @Bean
    public TokenStore tokenStore() {
        if (storeWithJwt) {
            return new JwtTokenStore(accessTokenConverter());
        }
        return new RedisTokenStore(redisConnectionFactory) {
            @Override
            public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
                if (allowUniqeToken) {
                    super.getAccessToken(authentication);
                }
                return null;
            }
        };
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.authenticationManager(this.authenticationManager);
        endpoints.tokenStore(tokenStore());
        // ?????????????????????code??????
//		endpoints.authorizationCodeServices(new JdbcAuthorizationCodeServices(dataSource));
        endpoints.authorizationCodeServices(redisAuthorizationCodeServices);
        endpoints.exceptionTranslator(authResponseExceptionTranslator);
        if (storeWithJwt) {
            endpoints.accessTokenConverter(accessTokenConverter());
        } else {
            endpoints.tokenEnhancer(customTokenConverter);
        }
    }


    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security.allowFormAuthenticationForClients();
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
//		clients.inMemory().withClient("system").secret("system")
//				.authorizedGrantTypes("password", "authorization_code", "refresh_token").scopes("app")
//				.accessTokenValiditySeconds(3600);

        clients.jdbc(dataSource);
    }

    @Autowired
    public UserDetailsService userDetailsService;

    /**
     * Jwt?????????????????????
     *
     * @return accessTokenConverter
     */
    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        jwtAccessTokenConverter.setSigningKey(jwtSigningKey);
        DefaultAccessTokenConverter defaultAccessTokenConverter = (DefaultAccessTokenConverter) jwtAccessTokenConverter
                .getAccessTokenConverter();
        DefaultUserAuthenticationConverter userAuthenticationConverter = new DefaultUserAuthenticationConverter();
        userAuthenticationConverter.setUserDetailsService(userDetailsService);

        defaultAccessTokenConverter.setUserTokenConverter(userAuthenticationConverter);

        return jwtAccessTokenConverter;
    }

}
