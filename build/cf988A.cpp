/*#include<bits/stdc++.h>
using namespace std;
bool vis[124];
int a[120];
int k,n;
int main()
{
    cin>>k>>n;
    int cnt=0;
    for(int i=1;i<=k;i++){
        int x;
        cin>>x;
        if(!vis[x]){ a[cnt++]=i;vis[x]=true;}
        if(cnt>=k) break;
    }
    //cout<<cnt<<endl;
    if(cnt>=n){
        cout<<"YES"<<endl;
        for(int i=0;i<n;i++) cout<<a[i]<<" ";
        cout<<endl;
    }
    else cout<<"NO"<<endl;
    return 0;
}
*/
#include<bits/stdc++.h>
using namespace std;
map<int,int>mp;
int main()
{
    int n,k,x;
    cin>>n>>k;
    for(int i=1;i<=k;i++){
        cin>>x;
        mp[x]=i;
    }
    if(mp.size()<n) cout<<"NO"<<endl;
    else{
        cout<<"YES"<<endl;
        int i=0;
        for(auto it:mp) if(i++<n) cout<<it.second<<" ";
    }
    return 0;
}
